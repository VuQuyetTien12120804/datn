package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.common.AppointmentStatus;
import com.bookingcare.backend_bookingcare.common.StatusMapper;
import com.bookingcare.backend_bookingcare.entity.Appointment;
import com.bookingcare.backend_bookingcare.entity.Doctor;
import com.bookingcare.backend_bookingcare.entity.Patient;
import com.bookingcare.backend_bookingcare.repository.AppointmentRepository;
import com.bookingcare.backend_bookingcare.repository.DoctorRepository;
import com.bookingcare.backend_bookingcare.repository.PatientRepository;
import com.bookingcare.backend_bookingcare.util.DateTimeFormatUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DoctorPanelService {

    private static final ZoneOffset VN = ZoneOffset.ofHours(7);

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final AdminAppointmentService adminAppointmentService;
    private final AppointmentSlotService appointmentSlotService;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Map<String, Object> profile(int accountId) {
        Doctor doctor = requireDoctor(accountId);
        Map<String, Object> m = new HashMap<>();
        m.put("doctorId", doctor.getId());
        m.put("fullName", doctor.getFullName());
        m.put("email", doctor.getEmail());
        m.put("phone", doctor.getPhone());
        m.put("licenseNo", doctor.getLicenseNo());
        m.put("bio", doctor.getBio());
        m.put("roomLocation", doctor.getRoomLocation());
        m.put("scheduleText", doctor.getScheduleText());
        m.put("specialty", resolveDoctorSpecialty(doctor.getId()));
        m.put("clinicName", resolveClinicName());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> byStatus(int accountId, String apiStatus) {
        Doctor doctor = requireDoctor(accountId);
        List<Appointment> rows;
        if ("CANCELLED".equalsIgnoreCase(apiStatus)) {
            rows = appointmentRepository.findByDoctorIdAndStatusInOrderByStartsAtDesc(
                    doctor.getId(),
                    List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW));
        } else {
            String dbStatus = StatusMapper.toDb(apiStatus);
            rows = appointmentRepository.findByDoctorIdAndStatusOrderByStartsAtAsc(doctor.getId(), dbStatus);
        }
        return rows.stream()
                .map(this::toDoctorAppointmentRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> today(int accountId) {
        Doctor doctor = requireDoctor(accountId);
        LocalDate today = LocalDate.now(VN);
        OffsetDateTime start = DateTimeFormatUtil.dayStart(today);
        OffsetDateTime end = DateTimeFormatUtil.dayEnd(today);
        return appointmentRepository
                .findByDoctorIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        doctor.getId(), start, end)
                .stream()
                .filter(a -> !AppointmentStatus.CANCELLED.equals(AppointmentStatus.normalize(a.getStatus())))
                .filter(a -> !AppointmentStatus.PENDING.equals(AppointmentStatus.normalize(a.getStatus())))
                .map(this::toDoctorAppointmentRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(int accountId, int appointmentId) {
        Doctor doctor = requireDoctor(accountId);
        Appointment a = requireOwnedAppointment(doctor, appointmentId);
        return adminAppointmentService.detail(a.getId());
    }

    @Transactional
    public Map<String, Object> confirm(int accountId, int appointmentId) {
        Doctor doctor = requireDoctor(accountId);
        Appointment a = requireOwnedAppointment(doctor, appointmentId);
        if (!AppointmentStatus.canDoctorConfirm(a.getStatus())) {
            throw new ApiException(400, "Chỉ có thể chấp nhận lịch đang chờ duyệt");
        }
        if (a.getStartsAt().isBefore(OffsetDateTime.now(VN))) {
            throw new ApiException(400, "Không thể chấp nhận lịch đã qua giờ bắt đầu khám");
        }
        a.setStatus(AppointmentStatus.CONFIRMED);
        a.setConfirmedAt(OffsetDateTime.now(VN));
        a.setUpdatedAt(OffsetDateTime.now(VN));
        appointmentRepository.save(a);
        return adminAppointmentService.detail(a.getId());
    }

    @Transactional
    public Map<String, Object> cancel(int accountId, int appointmentId) {
        Doctor doctor = requireDoctor(accountId);
        Appointment a = requireOwnedAppointment(doctor, appointmentId);
        if (!AppointmentStatus.canDoctorCancel(a.getStatus())) {
            throw new ApiException(400, "Không thể từ chối lịch ở trạng thái hiện tại");
        }
        a.setStatus(AppointmentStatus.CANCELLED);
        a.setCancelledAt(OffsetDateTime.now(VN));
        a.setCancelReason("Doctor cancelled");
        a.setUpdatedAt(OffsetDateTime.now(VN));
        appointmentRepository.save(a);
        appointmentSlotService.syncBookedCount(a.getSlotId());
        return adminAppointmentService.detail(a.getId());
    }

    /** Bắt đầu khám: confirmed hoặc checked_in → checked_in. */
    @Transactional
    public Map<String, Object> startExam(int accountId, int appointmentId) {
        Doctor doctor = requireDoctor(accountId);
        Appointment a = requireOwnedAppointment(doctor, appointmentId);
        if (!AppointmentStatus.canDoctorStartExam(a.getStatus())) {
            throw new ApiException(400, "Chỉ có thể bắt đầu khám khi lịch đã được duyệt hoặc bệnh nhân đã check-in");
        }
        if (AppointmentStatus.isTerminal(a.getStatus())) {
            throw new ApiException(400, "Lịch hẹn đã kết thúc");
        }
        a.setStatus(AppointmentStatus.CHECKED_IN);
        if (a.getCheckedInAt() == null) {
            a.setCheckedInAt(OffsetDateTime.now(VN));
        }
        a.setUpdatedAt(OffsetDateTime.now(VN));
        appointmentRepository.save(a);
        appointmentSlotService.syncBookedCount(a.getSlotId());
        return adminAppointmentService.detail(a.getId());
    }

    /** Lưu ghi chú khám (chỉ khi đang khám). */
    @Transactional
    public Map<String, Object> saveClinicalNote(int accountId, int appointmentId, String note) {
        Doctor doctor = requireDoctor(accountId);
        Appointment a = requireOwnedAppointment(doctor, appointmentId);
        if (!AppointmentStatus.canDoctorSaveNote(a.getStatus())) {
            throw new ApiException(400, "Chỉ có thể ghi chú khi đang khám bệnh nhân");
        }
        a.setNote(note != null ? note.trim() : null);
        a.setUpdatedAt(OffsetDateTime.now(VN));
        appointmentRepository.save(a);
        return adminAppointmentService.detail(a.getId());
    }

    /** Hoàn thành khám: checked_in → completed. */
    @Transactional
    public Map<String, Object> completeExam(int accountId, int appointmentId, String note) {
        Doctor doctor = requireDoctor(accountId);
        Appointment a = requireOwnedAppointment(doctor, appointmentId);
        if (!AppointmentStatus.canDoctorComplete(a.getStatus())) {
            throw new ApiException(400, "Cần bắt đầu khám trước khi hoàn thành");
        }
        if (note != null && !note.isBlank()) {
            a.setNote(note.trim());
        }
        a.setStatus(AppointmentStatus.COMPLETED);
        a.setCompletedAt(OffsetDateTime.now(VN));
        a.setUpdatedAt(OffsetDateTime.now(VN));
        appointmentRepository.save(a);
        appointmentSlotService.syncBookedCount(a.getSlotId());
        return adminAppointmentService.detail(a.getId());
    }

    /** Đánh dấu bệnh nhân không đến (sau giờ hẹn). */
    @Transactional
    public Map<String, Object> markNoShow(int accountId, int appointmentId) {
        Doctor doctor = requireDoctor(accountId);
        Appointment a = requireOwnedAppointment(doctor, appointmentId);
        if (!AppointmentStatus.canDoctorMarkNoShow(a.getStatus())) {
            throw new ApiException(400, "Chỉ có thể đánh dấu không đến với lịch đã duyệt");
        }
        if (!a.getStartsAt().isBefore(OffsetDateTime.now(VN))) {
            throw new ApiException(400, "Chưa đến giờ hẹn — chưa thể đánh dấu không đến");
        }
        a.setStatus(AppointmentStatus.NO_SHOW);
        a.setUpdatedAt(OffsetDateTime.now(VN));
        appointmentRepository.save(a);
        appointmentSlotService.syncBookedCount(a.getSlotId());
        return adminAppointmentService.detail(a.getId());
    }

    private Appointment requireOwnedAppointment(Doctor doctor, int appointmentId) {
        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy lịch hẹn"));
        if (!doctor.getId().equals(a.getDoctorId())) {
            throw new ApiException(403, "Forbidden");
        }
        return a;
    }

    private Doctor requireDoctor(int accountId) {
        return doctorRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ApiException(403, "Tài khoản không phải bác sĩ"));
    }

    private Map<String, Object> toDoctorAppointmentRow(Appointment a) {
        Map<String, Object> m = new HashMap<>();
        m.put("appointmentId", a.getId());
        m.put("patientId", a.getPatientId());
        patientRepository.findById(a.getPatientId()).ifPresent(p -> enrichPatient(m, p));
        m.put("appointmentDate", DateTimeFormatUtil.toIsoDate(a.getStartsAt()));
        m.put("expectedTime", DateTimeFormatUtil.toTimeHm(a.getStartsAt()));
        m.put("status", StatusMapper.toApi(a.getStatus()));
        m.put("reason", a.getReason());
        m.put("clinicalNote", a.getNote());
        // Giữ notes cho tương thích cũ — ưu tiên lý do đăng ký
        m.put("notes", a.getReason() != null && !a.getReason().isBlank() ? a.getReason() : a.getNote());
        return m;
    }

    private static void enrichPatient(Map<String, Object> m, Patient p) {
        m.put("patientName", p.getFullName());
        m.put("phoneNumber", p.getPhone());
        m.put("phone", p.getPhone());
        m.put("email", p.getEmail());
        m.put("gender", p.getGender());
        m.put("address", p.getAddress());
        if (p.getDob() != null) {
            m.put("dob", DateTimeFormatUtil.toDdMmYyyy(p.getDob()));
        }
    }

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

    private String resolveClinicName() {
        Query q = em.createNativeQuery("SELECT TOP 1 name FROM dbo.clinics ORDER BY id");
        List<?> rows = q.getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return null;
        }
        return rows.get(0).toString();
    }
}
