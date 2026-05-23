package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Integer> {

    Optional<Doctor> findByAccountId(Integer accountId);
}
