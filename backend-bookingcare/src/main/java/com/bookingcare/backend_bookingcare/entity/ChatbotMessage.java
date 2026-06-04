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
 * Một tin nhắn trong phiên chatbot. Sender thuộc {user, assistant, system, tool}.
 * meta lưu JSON phụ (intent, confidence, suggestions) — phục vụ phân tích sau này.
 */
@Entity
@Table(name = "chatbot_messages")
@Getter
@Setter
public class ChatbotMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "session_id", nullable = false)
    private Integer sessionId;

    @Column(nullable = false, length = 20)
    private String sender;

    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    @Column(length = 100)
    private String model;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String meta = "{}";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
