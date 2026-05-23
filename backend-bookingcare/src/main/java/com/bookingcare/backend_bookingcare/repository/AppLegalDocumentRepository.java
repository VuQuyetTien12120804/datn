package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.AppLegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppLegalDocumentRepository extends JpaRepository<AppLegalDocument, Integer> {

    Optional<AppLegalDocument> findByCodeIgnoreCase(String code);
}
