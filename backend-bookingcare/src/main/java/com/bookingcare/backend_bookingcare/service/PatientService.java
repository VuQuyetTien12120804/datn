package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.common.AppointmentStatus;
import com.bookingcare.backend_bookingcare.common.StatusMapper;
import com.bookingcare.backend_bookingcare.entity.Account;
import com.bookingcare.backend_bookingcare.entity.Appointment;
import com.bookingcare.backend_bookingcare.entity.Patient;
import com.bookingcare.backend_bookingcare.repository.AccountRepository;
import com.bookingcare.backend_bookingcare.repository.AppointmentRepository;
import com.bookingcare.backend_bookingcare.repository.DoctorRepository;
import com.bookingcare.backend_bookingcare.repository.PatientRepository;
import com.bookingcare.backend_bookingcare.util.DateTimeFormatUtil;
import com.bookingcare.backend_bookingcare.util.GenderUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class PatientService {

    private static final Set<String> UPCOMING = Set.of("pending", "confirmed", "checked_in");
    private static final Set<String> CANCELLED_GROUP = Set.of("cancelled", "no_show");

    private final PatientRepository patientRepository;
    private final AccountRepository accountRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentSlotService appointmentSlotService;
    private final AppointmentQueueService appointmentQueueService;
    private final AppointmentExpiryService appointmentExpiryService;

    private static final ZoneOffset VN = ZoneOffset.ofHours(7);

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(int accountId) {
        Patient patient = resolvePatient(accountId);
        return toProfileMap(patient);
    }

    @Transactional
    public Map<String, Object> updateProfile(int accountId, String phone, String dob, String gender, String address) {
        Patient patient = resolvePatient(accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy tài khoản"));

        if (phone != null) {
            patient.setPhone(phone);
            account.setPhone(phone);
        }
        if (dob != null && !dob.isBlank()) {
            patient.setDob(DateTimeFormatUtil.parseDdMmYyyy(dob));
        }
        if (gender != null && !gender.isBlank()) {
            patient.setGender(GenderUtil.normalize(gender));
        }
        if (address != null) {
            patient.setAddress(address);
        }
        patient.setUpdatedAt(OffsetDateTime.now());
        account.setUpdatedAt(OffsetDateTime.now());
        patientRepository.save(patient);
        accountRepository.save(account);
        return toProfileMap(patient);
    }

    @Transactional
    public List<Map<String, Object>> listAppointments(int accountId, String group) {
        appointmentExpiryService.expireUnconfirmedPastPending();

        Patient patient = patientRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ApiException(404, "Chưa có hồ sơ bệnh nhân"));

        String normalizedGroup = group != null ? group.trim().toUpperCase(Locale.ROOT) : "UPCOMING";
        OffsetDateTime now = OffsetDateTime.now(VN);
        List<Appointment> all = appointmentRepository.findByPatientIdOrderByStartsAtDesc(patient.getId());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Appointment a : all) {
            if (!matchesGroup(a, normalizedGroup, now)) {
                continue;
            }
            out.add(toAppointmentMap(a));
        }
        return out;
    }

    @Transactional
    public void cancelAppointment(int accountId, int appointmentId, String cancelReason) {
        Patient patient = patientRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ApiException(404, "Chưa có hồ sơ bệnh nhân"));
        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy lịch hẹn"));
        if (!patient.getId().equals(a.getPatientId())) {
            throw new ApiException(403, "Forbidden");
        }
        if (!AppointmentStatus.canPatientCancel(a.getStatus())) {
            throw new ApiException(400, "Không thể hủy lịch hẹn này");
        }
        a.setStatus(AppointmentStatus.CANCELLED);
        a.setCancelReason(cancelReason != null && !cancelReason.isBlank() ? cancelReason : "Patient cancelled");
        a.setCancelledAt(OffsetDateTime.now());
        a.setUpdatedAt(OffsetDateTime.now());
        appointmentRepository.save(a);
        appointmentSlotService.syncBookedCount(a.getSlotId());
    }

    /** Bệnh nhân check-in tại phòng khám: confirmed → checked_in (trong ngày hẹn). */
    @Transactional
    public Map<String, Object> checkInAppointment(int accountId, int appointmentId) {
        Patient patient = patientRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ApiException(404, "Chưa có hồ sơ bệnh nhân"));
        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy lịch hẹn"));
        if (!patient.getId().equals(a.getPatientId())) {
            throw new ApiException(403, "Forbidden");
        }
        if (!AppointmentStatus.canPatientCheckIn(a.getStatus())) {
            throw new ApiException(400, "Chỉ có thể check-in khi lịch đã được bác sĩ duyệt");
        }
        java.time.ZoneOffset vn = java.time.ZoneOffset.ofHours(7);
        LocalDate today = LocalDate.now(vn);
        LocalDate apptDay = a.getStartsAt().atZoneSameInstant(vn).toLocalDate();
        if (!today.equals(apptDay)) {
            throw new ApiException(400, "Chỉ check-in được trong ngày khám (" + DateTimeFormatUtil.toDdMmYyyy(apptDay) + ")");
        }
        a.setStatus(AppointmentStatus.CHECKED_IN);
        a.setCheckedInAt(OffsetDateTime.now(vn));
        a.setUpdatedAt(OffsetDateTime.now(vn));
        appointmentRepository.save(a);
        return getAppointmentDetail(accountId, appointmentId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAppointmentDetail(int accountId, int appointmentId) {
        Patient patient = patientRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ApiException(404, "Chưa có hồ sơ bệnh nhân"));
        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy lịch hẹn"));
        if (!patient.getId().equals(a.getPatientId())) {
            throw new ApiException(403, "Forbidden");
        }
        Map<String, Object> m = toAppointmentMap(a);
        m.put("patientName", patient.getFullName());
        m.put("patientPhone", patient.getPhone());
        m.put("patientEmail", patient.getEmail());
        m.put("patientGender", patient.getGender());
        m.put("patientDob", DateTimeFormatUtil.toDdMmYyyy(patient.getDob()));
        m.put("patientAddress", patient.getAddress());
        m.put("cancelReason", a.getCancelReason());
        m.put("note", a.getNote());
        return m;
    }

    private boolean matchesGroup(Appointment a, String group, OffsetDateTime now) {
        String s = a.getStatus() != null ? a.getStatus().toLowerCase(Locale.ROOT) : "pending";
        return switch (group) {
            case "COMPLETED" -> "completed".equals(s);
            case "CANCELLED" -> CANCELLED_GROUP.contains(s);
            default -> UPCOMING.contains(s)
                    && a.getEndsAt() != null
                    && !a.getEndsAt().isBefore(now);
        };
    }

    private Map<String, Object> toAppointmentMap(Appointment a) {
        Map<String, Object> m = new HashMap<>();
        m.put("appointmentId", a.getId());
        m.put("doctorId", a.getDoctorId());
        doctorRepository.findById(a.getDoctorId()).ifPresent(d -> m.put("doctorName", d.getFullName()));
        m.put("specialty", resolveDoctorSpecialty(a.getDoctorId()));
        m.put("appointmentDate", DateTimeFormatUtil.toIsoDate(a.getStartsAt()));
        m.put("startTime", DateTimeFormatUtil.toTimeHm(a.getStartsAt()));
        m.put("endTime", DateTimeFormatUtil.toTimeHm(a.getEndsAt()));
        m.put("status", StatusMapper.toApi(a.getStatus()));
        m.put("reason", a.getReason());
        m.put("cancelReason", a.getCancelReason());
        m.put("clinicalNote", a.getNote());
        Integer queueNo = appointmentQueueService.queueNumberFor(a.getId());
        if (queueNo != null) {
            m.put("queueNumber", queueNo);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private String resolveDoctorSpecialty(int doctorId) {
        Query q = em.createNativeQuery("""
                SELECT TOP 1 s.name
                FROM dbo.doctor_specialties ds
                JOIN dbo.specialties s ON s.id = ds.specialty_id
                WHERE ds.doctor_id = :doctorId
                ORDER BY s.id
                """);
        q.setParameter("doctorId", doctorId);
        List<?> rows = q.getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return null;
        }
        return rows.get(0).toString();
    }

    private Map<String, Object> toProfileMap(Patient patient) {
        Map<String, Object> m = new HashMap<>();
        m.put("patientId", patient.getId());
        m.put("fullName", patient.getFullName());
        m.put("email", patient.getEmail());
        m.put("phone", patient.getPhone());
        m.put("dob", DateTimeFormatUtil.toDdMmYyyy(patient.getDob()));
        m.put("gender", patient.getGender());
        m.put("address", patient.getAddress());
        return m;
    }

    private Patient resolvePatient(int accountId) {
        return patientRepository.findByAccountId(accountId)
                .orElseGet(() -> createPatientFromAccount(accountId));
    }

    private Patient createPatientFromAccount(int accountId) {
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
}
