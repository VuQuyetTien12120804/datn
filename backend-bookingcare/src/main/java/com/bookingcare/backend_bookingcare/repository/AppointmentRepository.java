package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer>, JpaSpecificationExecutor<Appointment> {

    List<Appointment> findByPatientIdOrderByStartsAtDesc(Integer patientId);

    List<Appointment> findByDoctorIdAndStatusOrderByStartsAtDesc(Integer doctorId, String status);

    List<Appointment> findByDoctorIdAndStatusInOrderByStartsAtDesc(Integer doctorId, Collection<String> statuses);

    List<Appointment> findByDoctorIdAndStatusOrderByStartsAtAsc(Integer doctorId, String status);

    List<Appointment> findByDoctorIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            Integer doctorId, OffsetDateTime startInclusive, OffsetDateTime endExclusive);

    long countBySlotIdAndStatusIn(Integer slotId, Collection<String> statuses);

    boolean existsByPatientIdAndDoctorIdAndStatusNotIn(Integer patientId, Integer doctorId, Collection<String> statuses);

    List<Appointment> findByStatusAndEndsAtBefore(String status, OffsetDateTime endsAt);
}
