package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.ChatbotSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChatbotSessionRepository extends JpaRepository<ChatbotSession, Integer> {

    /** Lấy session đang mở gần nhất của 1 account (chưa đóng). */
    @Query("SELECT s FROM ChatbotSession s WHERE s.accountId = :accountId AND s.isClosed = false ORDER BY s.updatedAt DESC")
    Optional<ChatbotSession> findActiveByAccount(Integer accountId);
}
