package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.MessageThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MessageThreadRepository extends JpaRepository<MessageThread, Integer> {

    Optional<MessageThread> findByPatientIdAndDoctorId(Integer patientId, Integer doctorId);

    Optional<MessageThread> findByPatientIdAndThreadType(Integer patientId, String threadType);

    List<MessageThread> findByPatientIdOrderByLastMessageAtDescUpdatedAtDesc(Integer patientId);

    List<MessageThread> findByDoctorIdOrderByLastMessageAtDescUpdatedAtDesc(Integer doctorId);

    List<MessageThread> findByDoctorIdInOrderByLastMessageAtDescUpdatedAtDesc(Collection<Integer> doctorIds);

    List<MessageThread> findByThreadTypeOrderByLastMessageAtDescUpdatedAtDesc(String threadType);
}
