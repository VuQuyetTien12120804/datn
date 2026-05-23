package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.AppointmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Integer> {

    @Query("""
            SELECT s FROM AppointmentSlot s
            WHERE s.doctorId = :doctorId
              AND s.active = true
              AND s.startsAt >= :dayStart
              AND s.startsAt < :dayEnd
            ORDER BY s.startsAt
            """)
    List<AppointmentSlot> findActiveByDoctorAndDay(
            @Param("doctorId") int doctorId,
            @Param("dayStart") OffsetDateTime dayStart,
            @Param("dayEnd") OffsetDateTime dayEnd
    );

    @Query(value = """
            SELECT DISTINCT CONVERT(varchar(10), starts_at, 23)
            FROM dbo.appointment_slots
            WHERE doctor_id = :doctorId AND is_active = 1 AND starts_at >= :from
            ORDER BY 1
            """, nativeQuery = true)
    List<String> findWorkingDateStrings(@Param("doctorId") int doctorId, @Param("from") OffsetDateTime from);
}
