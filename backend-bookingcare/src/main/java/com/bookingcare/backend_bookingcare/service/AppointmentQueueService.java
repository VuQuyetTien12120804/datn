package com.bookingcare.backend_bookingcare.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Số thứ tự khám trong ngày theo từng bác sĩ (thứ tự {@code starts_at}).
 * Không lưu cột riêng — tính động từ lịch đã đặt (trừ cancelled/no_show).
 */
@Service
public class AppointmentQueueService {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Integer queueNumberFor(int appointmentId) {
        Query q = em.createNativeQuery("""
                SELECT rn FROM (
                  SELECT a.id,
                    ROW_NUMBER() OVER (
                      PARTITION BY a.doctor_id, CAST(SWITCHOFFSET(a.starts_at, '+07:00') AS date)
                      ORDER BY a.starts_at ASC, a.id ASC
                    ) AS rn
                  FROM dbo.appointments a
                  WHERE a.status NOT IN (N'cancelled', N'no_show')
                ) x WHERE x.id = :apptId
                """);
        q.setParameter("apptId", appointmentId);
        @SuppressWarnings("unchecked")
        List<Object> rows = q.getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return null;
        }
        return ((Number) rows.get(0)).intValue();
    }
}
