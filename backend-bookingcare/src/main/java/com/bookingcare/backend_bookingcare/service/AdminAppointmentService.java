package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.common.AppointmentStatus;
import com.bookingcare.backend_bookingcare.common.PageResponse;
import com.bookingcare.backend_bookingcare.common.StatusMapper;
import com.bookingcare.backend_bookingcare.entity.Appointment;
import com.bookingcare.backend_bookingcare.entity.AppointmentSlot;
import com.bookingcare.backend_bookingcare.repository.AppointmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookingcare.backend_bookingcare.util.DateTimeFormatUtil;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlotService appointmentSlotService;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Integer doctorId, Integer patientId, String apiStatus) {
        String dbStatus = apiStatus != null && !apiStatus.isBlank() ? StatusMapper.toDb(apiStatus) : null;
        return appointmentRepository.findAll(Sort.by(Sort.Direction.DESC, "startsAt")).stream()
                .filter(a -> doctorId == null || doctorId.equals(a.getDoctorId()))
                .filter(a -> patientId == null || patientId.equals(a.getPatientId()))
                .filter(a -> dbStatus == null || dbStatus.equals(AppointmentStatus.normalize(a.getStatus())))
                .limit(500)
                .map(this::toAppointmentMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> search(
            Integer doctorId,
            Integer patientId,
            String status,
            String q,
            String fromDate,
            String toDate,
            int page,
            int size
    ) {
        String dbStatus = status != null && !status.isBlank() ? StatusMapper.toDb(status) : null;
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();
        if (doctorId != null) {
            where.append(" AND a.doctor_id = :doctorId ");
            params.put("doctorId", doctorId);
        }
        if (patientId != null) {
            where.append(" AND a.patient_id = :patientId ");
            params.put("patientId", patientId);
        }
        if (dbStatus != null) {
            where.append(" AND a.status = :status ");
            params.put("status", dbStatus);
        }
        if (fromDate != null && !fromDate.isBlank()) {
            where.append(" AND CAST(a.starts_at AS date) >= :fromDate ");
            params.put("fromDate", java.time.LocalDate.parse(fromDate));
        }
        if (toDate != null && !toDate.isBlank()) {
            where.append(" AND CAST(a.starts_at AS date) <= :toDate ");
            params.put("toDate", java.time.LocalDate.parse(toDate));
        }
        if (q != null && !q.isBlank()) {
            where.append(" AND (p.full_name LIKE :q OR d.full_name LIKE :q OR CAST(a.id AS nvarchar(20)) LIKE :q) ");
            params.put("q", "%" + q.trim() + "%");
        }

        String base = """
                FROM dbo.appointments a
                LEFT JOIN dbo.patients p ON p.id = a.patient_id
                LEFT JOIN dbo.doctors d ON d.id = a.doctor_id
                LEFT JOIN dbo.services sv ON sv.id = a.service_id
                LEFT JOIN dbo.rooms r ON r.id = a.room_id
                """ + where;

        Query countQ = em.createNativeQuery("SELECT COUNT(*) " + base);
        params.forEach(countQ::setParameter);
        long total = ((Number) countQ.getSingleResult()).longValue();

        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        int offset = safePage * safeSize;

        String sql = """
                SELECT
                  a.id AS appointment_id,
                  a.patient_id,
                  p.full_name AS patient_name,
                  a.doctor_id,
                  d.full_name AS doctor_name,
                  a.service_id,
                  sv.name AS service_name,
                  a.room_id,
                  r.name AS room_name,
                  a.slot_id,
                  a.starts_at,
                  a.ends_at,
                  a.status
                """ + base + """
                ORDER BY a.starts_at DESC
                OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """;

        Query dataQ = em.createNativeQuery(sql);
        params.forEach(dataQ::setParameter);
        dataQ.setParameter("offset", offset);
        dataQ.setParameter("limit", safeSize);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQ.getResultList();
        List<Map<String, Object>> content = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("appointmentId", ((Number) r[0]).intValue());
            m.put("patientId", r[1] != null ? ((Number) r[1]).intValue() : null);
            m.put("patientName", r[2] != null ? r[2].toString() : null);
            m.put("doctorId", r[3] != null ? ((Number) r[3]).intValue() : null);
            m.put("doctorName", r[4] != null ? r[4].toString() : null);
            m.put("serviceId", r[5] != null ? ((Number) r[5]).intValue() : null);
            m.put("serviceName", r[6] != null ? r[6].toString() : null);
            m.put("roomId", r[7] != null ? ((Number) r[7]).intValue() : null);
            m.put("roomName", r[8] != null ? r[8].toString() : null);
            m.put("slotId", r[9] != null ? ((Number) r[9]).intValue() : null);
            m.put("startsAt", r[10] != null ? r[10].toString() : null);
            m.put("endsAt", r[11] != null ? r[11].toString() : null);
            m.put("status", StatusMapper.toApi(r[12] != null ? r[12].toString() : null));
            content.add(m);
        }

        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return PageResponse.<Map<String, Object>>builder()
                .content(content)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .first(safePage == 0)
                .last(safePage >= totalPages - 1)
                .empty(content.isEmpty())
                .numberOfElements(content.size())
                .sorted(true)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(int id) {
        String sql = """
                SELECT
                  a.id, a.status, a.starts_at, a.ends_at,
                  a.doctor_id, d.full_name,
                  a.patient_id, p.full_name, p.phone, p.email, p.gender, p.dob, p.address,
                  a.slot_id, a.room_id, r.name,
                  a.service_id, sv.name, sv.duration_minutes,
                  a.reason, a.note, a.cancel_reason
                FROM dbo.appointments a
                LEFT JOIN dbo.patients p ON p.id = a.patient_id
                LEFT JOIN dbo.doctors d ON d.id = a.doctor_id
                LEFT JOIN dbo.rooms r ON r.id = a.room_id
                LEFT JOIN dbo.services sv ON sv.id = a.service_id
                WHERE a.id = :id
                """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("id", id);
        try {
            Object[] r = (Object[]) q.getSingleResult();
            Map<String, Object> m = new HashMap<>();
            m.put("appointmentId", ((Number) r[0]).intValue());
            m.put("status", StatusMapper.toApi(r[1].toString()));
            m.put("startsAt", r[2] != null ? r[2].toString() : null);
            m.put("endsAt", r[3] != null ? r[3].toString() : null);
            m.put("doctorId", r[4] != null ? ((Number) r[4]).intValue() : null);
            m.put("doctorName", r[5] != null ? r[5].toString() : null);
            m.put("patientId", r[6] != null ? ((Number) r[6]).intValue() : null);
            m.put("patientName", r[7] != null ? r[7].toString() : null);
            m.put("patientPhone", r[8] != null ? r[8].toString() : null);
            m.put("patientEmail", r[9] != null ? r[9].toString() : null);
            m.put("patientGender", r[10] != null ? r[10].toString() : null);
            m.put("patientDob", r[11] != null ? r[11].toString() : null);
            m.put("patientAddress", r[12] != null ? r[12].toString() : null);
            m.put("slotId", r[13] != null ? ((Number) r[13]).intValue() : null);
            m.put("roomId", r[14] != null ? ((Number) r[14]).intValue() : null);
            m.put("roomName", r[15] != null ? r[15].toString() : null);
            m.put("serviceId", r[16] != null ? ((Number) r[16]).intValue() : null);
            m.put("serviceName", r[17] != null ? r[17].toString() : null);
            m.put("serviceDurationMinutes", r[18] != null ? ((Number) r[18]).intValue() : null);
            m.put("reason", r[19] != null ? r[19].toString() : null);
            m.put("note", r[20] != null ? r[20].toString() : null);
            m.put("clinicalNote", r[20] != null ? r[20].toString() : null);
            m.put("cancelReason", r[21] != null ? r[21].toString() : null);
            if (r[2] != null) {
                try {
                    OffsetDateTime starts = OffsetDateTime.parse(r[2].toString());
                    m.put("appointmentDate", DateTimeFormatUtil.toIsoDate(starts));
                    m.put("expectedTime", DateTimeFormatUtil.toTimeHm(starts));
                } catch (Exception ignored) {
                }
            }
            return m;
        } catch (Exception e) {
            throw new ApiException(404, "Không tìm thấy lịch hẹn");
        }
    }

    @Transactional
    public Map<String, Object> reschedule(int id, int slotId) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy lịch hẹn"));
        AppointmentSlot slot = em.find(AppointmentSlot.class, slotId);
        if (slot == null) {
            throw new ApiException(404, "Không tìm thấy slot");
        }
        if (!slot.getDoctorId().equals(a.getDoctorId())) {
            throw new ApiException(400, "Slot phải thuộc cùng bác sĩ");
        }
        if (!Boolean.TRUE.equals(slot.getActive())) {
            throw new ApiException(400, "Slot không còn khả dụng");
        }
        Integer oldSlotId = a.getSlotId();
        if (oldSlotId == null || !oldSlotId.equals(slotId)) {
            long active = appointmentRepository.countBySlotIdAndStatusIn(
                    slotId, AppointmentSlotService.ACTIVE_BOOKING_STATUSES);
            int capacity = slot.getCapacity() != null ? slot.getCapacity() : 1;
            if (active >= capacity) {
                throw new ApiException(400, "Slot đã đầy");
            }
        }
        a.setSlotId(slotId);
        a.setStartsAt(slot.getStartsAt());
        a.setEndsAt(slot.getEndsAt());
        a.setRoomId(slot.getRoomId());
        a.setUpdatedAt(OffsetDateTime.now(DateTimeFormatUtil.CLINIC_OFFSET));
        appointmentRepository.save(a);
        if (oldSlotId != null && !oldSlotId.equals(slotId)) {
            appointmentSlotService.syncBookedCount(oldSlotId);
            appointmentSlotService.syncBookedCount(slotId);
        }
        return toAppointmentMap(a);
    }

    @Transactional
    public Map<String, Object> updateStatus(int id, String apiStatus, String note, String cancelReason) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy lịch hẹn"));
        OffsetDateTime now = OffsetDateTime.now(DateTimeFormatUtil.CLINIC_OFFSET);
        String prevStatus = AppointmentStatus.normalize(a.getStatus());
        String dbStatus = StatusMapper.toDb(apiStatus);
        a.setStatus(dbStatus);
        if (note != null) {
            a.setNote(note);
        }
        if (cancelReason != null) {
            a.setCancelReason(cancelReason);
        }
        switch (dbStatus) {
            case AppointmentStatus.CONFIRMED -> {
                if (a.getConfirmedAt() == null) {
                    a.setConfirmedAt(now);
                }
            }
            case AppointmentStatus.CHECKED_IN -> {
                if (a.getCheckedInAt() == null) {
                    a.setCheckedInAt(now);
                }
            }
            case AppointmentStatus.COMPLETED -> a.setCompletedAt(now);
            case AppointmentStatus.CANCELLED -> a.setCancelledAt(now);
            default -> {
            }
        }
        a.setUpdatedAt(now);
        appointmentRepository.save(a);
        boolean wasTerminal = AppointmentStatus.isTerminal(prevStatus);
        boolean nowActive = AppointmentStatus.PENDING.equals(dbStatus)
                || AppointmentStatus.CONFIRMED.equals(dbStatus)
                || AppointmentStatus.CHECKED_IN.equals(dbStatus);
        if (AppointmentStatus.CANCELLED.equals(dbStatus)
                || AppointmentStatus.NO_SHOW.equals(dbStatus)
                || AppointmentStatus.COMPLETED.equals(dbStatus)
                || (wasTerminal && nowActive)) {
            appointmentSlotService.syncBookedCount(a.getSlotId());
        }
        return toAppointmentMap(a);
    }

    private Map<String, Object> toAppointmentMap(Appointment a) {
        Map<String, Object> m = new HashMap<>();
        m.put("appointmentId", a.getId());
        m.put("patientId", a.getPatientId());
        m.put("doctorId", a.getDoctorId());
        m.put("serviceId", a.getServiceId());
        m.put("slotId", a.getSlotId());
        m.put("roomId", a.getRoomId());
        m.put("startsAt", a.getStartsAt() != null ? a.getStartsAt().toString() : null);
        m.put("endsAt", a.getEndsAt() != null ? a.getEndsAt().toString() : null);
        m.put("status", StatusMapper.toApi(a.getStatus()));
        m.put("reason", a.getReason());
        m.put("note", a.getNote());
        m.put("cancelReason", a.getCancelReason());
        m.put("cancelledAt", a.getCancelledAt() != null ? a.getCancelledAt().toString() : null);
        m.put("confirmedAt", a.getConfirmedAt() != null ? a.getConfirmedAt().toString() : null);
        m.put("checkedInAt", a.getCheckedInAt() != null ? a.getCheckedInAt().toString() : null);
        m.put("completedAt", a.getCompletedAt() != null ? a.getCompletedAt().toString() : null);
        m.put("createdByAccountId", a.getCreatedByAccountId());
        m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        m.put("updatedAt", a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null);
        return m;
    }
}
