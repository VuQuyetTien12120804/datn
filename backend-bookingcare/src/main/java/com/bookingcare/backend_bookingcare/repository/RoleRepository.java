package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByCodeIgnoreCase(String code);
}
