package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.ChatbotMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Integer> {

    /** Lấy toàn bộ tin nhắn của 1 session theo thứ tự thời gian. */
    List<ChatbotMessage> findBySessionIdOrderByCreatedAtAsc(Integer sessionId);
}
