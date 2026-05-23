package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.ClinicService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicServiceRepository extends JpaRepository<ClinicService, Integer> {

    List<ClinicService> findBySpecialtyId(Integer specialtyId);
}
