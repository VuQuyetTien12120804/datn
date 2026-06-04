package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.common.StatusMapper;
import com.bookingcare.backend_bookingcare.dto.BookingRequestDto;
import com.bookingcare.backend_bookingcare.entity.Account;
import com.bookingcare.backend_bookingcare.entity.Appointment;
import com.bookingcare.backend_bookingcare.entity.AppointmentSlot;
import com.bookingcare.backend_bookingcare.entity.Patient;
import com.bookingcare.backend_bookingcare.repository.AccountRepository;
import com.bookingcare.backend_bookingcare.repository.AppointmentRepository;
import com.bookingcare.backend_bookingcare.repository.AppointmentSlotRepository;
import com.bookingcare.backend_bookingcare.repository.PatientRepository;
import com.bookingcare.backend_bookingcare.util.DateTimeFormatUtil;
import com.bookingcare.backend_bookingcare.util.GenderUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int MIN_LEAD_MINUTES = 30;
    private static final Set<String> ACTIVE_BOOKING_STATUSES = AppointmentSlotService.ACTIVE_BOOKING_STATUSES;

    private final AppointmentSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final AccountRepository accountRepository;
    private final AppointmentSlotService appointmentSlotService;
    private final AppointmentQueueService appointmentQueueService;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<String> workingDates(int doctorId) {
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.ofHours(7)).minusHours(1);
        List<String> dates = slotRepository.findWorkingDateStrings(doctorId, from);
        return dates != null ? dates : List.of();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> slots(int doctorId, String slotDate) {
        LocalDate day = DateTimeFormatUtil.parseIsoDate(slotDate);
        if (day == null) {
            throw new ApiException(400, "slotDate không hợp lệ");
        }
        List<AppointmentSlot> slots = slotRepository.findActiveByDoctorAndDay(
                doctorId,
                DateTimeFormatUtil.dayStart(day),
                DateTimeFormatUtil.dayEnd(day)
        );
        List<Map<String, Object>> out = new ArrayList<>();
        OffsetDateTime minStart = OffsetDateTime.now(DateTimeFormatUtil.CLINIC_OFFSET).plusMinutes(MIN_LEAD_MINUTES);
        for (AppointmentSlot s : slots) {
            OffsetDateTime start = DateTimeFormatUtil.toClinicOffset(s.getStartsAt());
            boolean available = isSlotAvailable(s) && !start.isBefore(minStart);
            Map<String, Object> m = new HashMap<>();
            m.put("doctorId", s.getDoctorId());
            m.put("slotId", s.getId());
            m.put("slotDate", DateTimeFormatUtil.toIsoDate(s.getStartsAt()));
            m.put("startTime", DateTimeFormatUtil.toTimeHm(s.getStartsAt()));
            m.put("endTime", DateTimeFormatUtil.toTimeHm(s.getEndsAt()));
            m.put("isAvailable", available);
            out.add(m);
        }
        return out;
    }

    @Transactional
    public Map<String, Object> book(int accountId, BookingRequestDto req) {
        if (req.getDoctorId() == null || req.getSlotId() == null) {
            throw new ApiException(400, "doctorId và slotId là bắt buộc");
        }

        Patient patient = patientRepository.findByAccountId(accountId)
                .orElseGet(() -> createPatientForAccount(accountId));
        syncPatientFromBooking(patient, accountId, req);

        AppointmentSlot slot = slotRepository.findById(req.getSlotId())
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy slot"));
        if (!slot.getDoctorId().equals(req.getDoctorId())) {
            throw new ApiException(400, "Slot không thuộc bác sĩ này");
        }
        if (!Boolean.TRUE.equals(slot.getActive())) {
            throw new ApiException(400, "Khung giờ không còn khả dụng");
        }
        if (req.getAppointmentDate() != null && !req.getAppointmentDate().isBlank()) {
            LocalDate reqDay = DateTimeFormatUtil.parseIsoDate(req.getAppointmentDate());
            LocalDate slotDay = DateTimeFormatUtil.parseIsoDate(DateTimeFormatUtil.toIsoDate(slot.getStartsAt()));
            if (reqDay != null && slotDay != null && !reqDay.equals(slotDay)) {
                throw new ApiException(400, "appointmentDate không khớp với slot");
            }
        }
        if (!isSlotAvailable(slot)) {
            throw new ApiException(409, "Khung giờ đã được đặt");
        }

        OffsetDateTime slotStart = DateTimeFormatUtil.toClinicOffset(slot.getStartsAt());
        OffsetDateTime slotEnd = DateTimeFormatUtil.toClinicOffset(slot.getEndsAt());
        assertMinLead(slotStart);

        String notes = req.getNotes();

        int newId = callCreateAppointment(
                patient.getId(),
                req.getDoctorId(),
                null,
                req.getSlotId(),
                slot.getRoomId(),
                slotStart,
                slotEnd,
                notes,
                notes,
                accountId
        );

        appointmentSlotService.syncBookedCount(slot.getId());

        // Luôn chuẩn hóa giờ từ slot (+07) trước khi tính queue — tránh STT = 1 sai trên phiếu kết quả.
        Appointment saved = appointmentRepository.findById(newId)
                .orElseThrow(() -> new ApiException(500, "Không tạo được lịch hẹn"));
        OffsetDateTime slotStarts = DateTimeFormatUtil.toClinicOffset(slot.getStartsAt());
        OffsetDateTime slotEnds = DateTimeFormatUtil.toClinicOffset(slot.getEndsAt());
        saved.setStartsAt(slotStarts);
        saved.setEndsAt(slotEnds);
        appointmentRepository.saveAndFlush(saved);

        return buildBookingResponse(saved);
    }

    /** Gọi sau khi lịch đã flush — queue đếm đủ các lịch cùng BS trong ngày. */
    public Map<String, Object> buildBookingResponse(Appointment a) {
        Map<String, Object> m = new HashMap<>();
        m.put("appointmentId", a.getId());
        m.put("doctorId", a.getDoctorId());
        m.put("appointmentDate", DateTimeFormatUtil.toIsoDate(a.getStartsAt()));
        m.put("startTime", DateTimeFormatUtil.toTimeHm(a.getStartsAt()));
        m.put("endTime", DateTimeFormatUtil.toTimeHm(a.getEndsAt()));
        m.put("status", StatusMapper.toApi(a.getStatus()));
        m.put("notes", a.getNote());
        m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        Integer queueNo = appointmentQueueService.queueNumberFor(a.getId());
        if (queueNo != null) {
            m.put("queueNumber", queueNo);
        }
        return m;
    }

    private void syncPatientFromBooking(Patient patient, int accountId, BookingRequestDto req) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            patient.setFullName(req.getFullName().trim());
            if (account != null) {
                account.setFullName(req.getFullName().trim());
            }
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            patient.setEmail(req.getEmail().trim().toLowerCase(Locale.ROOT));
        } else if (account != null && account.getEmail() != null) {
            patient.setEmail(account.getEmail());
        }
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().isBlank()) {
            patient.setPhone(req.getPhoneNumber().trim());
            if (account != null) {
                account.setPhone(req.getPhoneNumber().trim());
            }
        }
        if (req.getDob() != null && !req.getDob().isBlank()) {
            LocalDate dob = req.getDob().contains("/")
                    ? DateTimeFormatUtil.parseDdMmYyyy(req.getDob())
                    : DateTimeFormatUtil.parseIsoDate(req.getDob());
            patient.setDob(dob);
        }
        if (req.getAddress() != null) {
            patient.setAddress(req.getAddress());
        }
        if (req.getGender() != null && !req.getGender().isBlank()) {
            patient.setGender(GenderUtil.normalize(req.getGender()));
        }
        patient.setUpdatedAt(OffsetDateTime.now());
        patientRepository.save(patient);
        if (account != null) {
            account.setUpdatedAt(OffsetDateTime.now());
            accountRepository.save(account);
        }
    }

    private Patient createPatientForAccount(int accountId) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy tài khoản"));
        Patient p = new Patient();
        p.setAccountId(accountId);
        p.setFullName(acc.getFullName());
        p.setEmail(acc.getEmail());
        p.setPhone(acc.getPhone());
        p.setGender("unknown");
        p.setCreatedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
        return patientRepository.save(p);
    }

    private boolean isSlotAvailable(AppointmentSlot s) {
        long active = appointmentRepository.countBySlotIdAndStatusIn(s.getId(), ACTIVE_BOOKING_STATUSES);
        int cap = s.getCapacity() != null ? s.getCapacity() : 1;
        return active < cap;
    }

    private static void assertMinLead(OffsetDateTime slotStart) {
        OffsetDateTime minStart = OffsetDateTime.now(DateTimeFormatUtil.CLINIC_OFFSET).plusMinutes(MIN_LEAD_MINUTES);
        if (slotStart.isBefore(minStart)) {
            throw new ApiException(400, "Phải đặt lịch trước ít nhất " + MIN_LEAD_MINUTES + " phút");
        }
    }

    private int callCreateAppointment(
            int patientId, int doctorId, Integer serviceId, Integer slotId, Integer roomId,
            OffsetDateTime startsAt, OffsetDateTime endsAt, String reason, String note, Integer createdBy
    ) {
        Session session = em.unwrap(Session.class);
        return session.doReturningWork(connection -> {
            try (CallableStatement cs = connection.prepareCall(
                    "{call dbo.sp_create_appointment(?,?,?,?,?,?,?,?,?,?,?)}")) {
                cs.setInt(1, patientId);
                cs.setInt(2, doctorId);
                if (serviceId != null) {
                    cs.setInt(3, serviceId);
                } else {
                    cs.setNull(3, Types.INTEGER);
                }
                cs.setInt(4, slotId);
                if (roomId != null) {
                    cs.setInt(5, roomId);
                } else {
                    cs.setNull(5, Types.INTEGER);
                }
                OffsetDateTime vnStart = DateTimeFormatUtil.toClinicOffset(startsAt);
                OffsetDateTime vnEnd = DateTimeFormatUtil.toClinicOffset(endsAt);
                cs.setObject(6, vnStart, Types.TIMESTAMP_WITH_TIMEZONE);
                cs.setObject(7, vnEnd, Types.TIMESTAMP_WITH_TIMEZONE);
                cs.setString(8, reason);
                cs.setString(9, note);
                if (createdBy != null) {
                    cs.setInt(10, createdBy);
                } else {
                    cs.setNull(10, Types.INTEGER);
                }
                cs.registerOutParameter(11, Types.INTEGER);
                cs.execute();
                return cs.getInt(11);
            }
        });
    }
}
