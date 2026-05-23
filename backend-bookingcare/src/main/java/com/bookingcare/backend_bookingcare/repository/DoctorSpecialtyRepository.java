package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.DoctorSpecialty;
import com.bookingcare.backend_bookingcare.entity.DoctorSpecialtyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorSpecialtyRepository extends JpaRepository<DoctorSpecialty, DoctorSpecialtyId> {

    List<DoctorSpecialty> findByDoctorIdIn(List<Integer> doctorIds);
}
