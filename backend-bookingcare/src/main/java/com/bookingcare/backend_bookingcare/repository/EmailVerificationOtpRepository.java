package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.EmailVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Integer> {

    Optional<EmailVerificationOtp> findTopByEmailIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String email, String purpose
    );

    Optional<EmailVerificationOtp> findTopByEmailIgnoreCaseAndPurposeAndConsumedAtIsNotNullOrderByConsumedAtDesc(
            String email, String purpose
    );
}
