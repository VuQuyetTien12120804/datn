package com.bookingcare.backend_bookingcare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Phiên hội thoại giữa người dùng và chatbot. Một bệnh nhân có thể có nhiều
 * session (mỗi lần mở app tạo 1 hoặc dùng lại session active gần nhất).
 */
@Entity
@Table(name = "chatbot_sessions")
@Getter
@Setter
public class ChatbotSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "patient_id")
    private Integer patientId;

    @Column(nullable = false, length = 20)
    private String channel = "mobile";

    @Column(length = 255)
    private String title;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = false;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
