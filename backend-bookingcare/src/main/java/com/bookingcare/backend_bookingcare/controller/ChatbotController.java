package com.bookingcare.backend_bookingcare.controller;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.dto.ChatbotMessageDto;
import com.bookingcare.backend_bookingcare.dto.ChatbotSendRequest;
import com.bookingcare.backend_bookingcare.security.CurrentAccountService;
import com.bookingcare.backend_bookingcare.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API cho chatbot rule-based. Dùng bởi app Android (vai trò PATIENT).
 */
@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final CurrentAccountService currentAccountService;

    /** Mở session: trả về sessionId đang dùng (tạo mới nếu chưa có session active). */
    @PostMapping("/sessions")
    public ApiEnvelope<Map<String, Object>> openSession() {
        int accountId = currentAccountService.requireUser().getAccountId();
        long sessionId = chatbotService.openSession(accountId);
        return ApiEnvelope.ok(Map.of("sessionId", sessionId));
    }

    /** Lấy lịch sử tin nhắn trong session — dùng khi mở app lại. */
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiEnvelope<List<ChatbotMessageDto>> listMessages(@PathVariable int sessionId) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(chatbotService.listMessages(accountId, sessionId));
    }

    /** Gửi 1 câu hỏi — backend trả về 2 message: user echo + assistant reply. */
    @PostMapping("/sessions/{sessionId}/messages")
    public ApiEnvelope<List<ChatbotMessageDto>> send(
            @PathVariable int sessionId,
            @Valid @RequestBody ChatbotSendRequest body) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(chatbotService.sendMessage(accountId, sessionId, body.getContent()));
    }
}
