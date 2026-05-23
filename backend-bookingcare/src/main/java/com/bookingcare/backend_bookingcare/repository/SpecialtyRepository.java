package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialtyRepository extends JpaRepository<Specialty, Integer> {
}
