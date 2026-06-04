package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.dto.ChatbotMessageDto;
import com.bookingcare.backend_bookingcare.entity.ChatbotMessage;
import com.bookingcare.backend_bookingcare.entity.ChatbotSession;
import com.bookingcare.backend_bookingcare.entity.Patient;
import com.bookingcare.backend_bookingcare.repository.ChatbotMessageRepository;
import com.bookingcare.backend_bookingcare.repository.ChatbotSessionRepository;
import com.bookingcare.backend_bookingcare.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service xử lý chatbot rule-based. Lưu session/messages vào CSDL phục vụ
 * truy vết và phân tích sau này; logic trả lời do {@link ChatbotIntentMatcher} đảm nhiệm.
 */
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatbotSessionRepository sessionRepository;
    private final ChatbotMessageRepository messageRepository;
    private final PatientRepository patientRepository;
    private final ChatbotIntentMatcher intentMatcher;

    /**
     * Mở session: dùng lại session active gần nhất nếu có, nếu không thì tạo mới
     * và ghi 1 message welcome của assistant.
     */
    @Transactional
    public Long openSession(int accountId) {
        ChatbotSession session = sessionRepository.findActiveByAccount(accountId)
                .orElseGet(() -> createNewSession(accountId));
        return session.getId().longValue();
    }

    /** Lấy toàn bộ tin nhắn của session — dùng khi mở app. */
    @Transactional(readOnly = true)
    public List<ChatbotMessageDto> listMessages(int accountId, int sessionId) {
        ChatbotSession session = requireOwned(accountId, sessionId);
        List<ChatbotMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<ChatbotMessageDto> out = new ArrayList<>(messages.size());
        for (ChatbotMessage m : messages) {
            out.add(toDto(m));
        }
        return out;
    }

    /**
     * Người dùng gửi 1 câu — service lưu user message, gọi intent matcher,
     * lưu assistant reply, trả về 2 message để client append vào danh sách.
     */
    @Transactional
    public List<ChatbotMessageDto> sendMessage(int accountId, int sessionId, String userText) {
        if (userText == null || userText.isBlank()) {
            throw new ApiException(400, "Nội dung tin nhắn không được rỗng");
        }
        ChatbotSession session = requireOwned(accountId, sessionId);
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Lưu tin nhắn người dùng
        ChatbotMessage userMsg = persistMessage(session.getId(), "user", userText, "{}", now);

        // 2. Match intent + lưu reply assistant (latency = thời gian xử lý thật)
        long t0 = System.currentTimeMillis();
        ChatbotIntentMatcher.Reply reply = intentMatcher.match(userText);
        int latencyMs = (int) (System.currentTimeMillis() - t0);

        String meta = String.format("{\"intent\":\"%s\"}", reply.intent());
        ChatbotMessage botMsg = persistMessage(session.getId(), "assistant", reply.content(), meta, OffsetDateTime.now());
        botMsg.setLatencyMs(latencyMs);
        botMsg.setModel("rule-based-v1");
        messageRepository.save(botMsg);

        // 3. Touch updated_at trên session để biết last activity
        session.setUpdatedAt(OffsetDateTime.now());
        sessionRepository.save(session);

        return List.of(
                toDto(userMsg),
                toAssistantDto(botMsg, reply.intent(), reply.suggestions())
        );
    }

    // ------------------- helpers -------------------

    private ChatbotSession createNewSession(int accountId) {
        Integer patientId = patientRepository.findByAccountId(accountId)
                .map(Patient::getId)
                .orElse(null);
        OffsetDateTime now = OffsetDateTime.now();

        ChatbotSession s = new ChatbotSession();
        s.setAccountId(accountId);
        s.setPatientId(patientId);
        s.setChannel("mobile");
        s.setTitle("Trợ lý AI ClinicBooking");
        s.setIsClosed(false);
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        s = sessionRepository.save(s);

        // Welcome message của assistant
        ChatbotIntentMatcher.Reply welcome = intentMatcher.welcomeReply();
        ChatbotMessage welcomeMsg = persistMessage(s.getId(), "assistant",
                welcome.content(), "{\"intent\":\"WELCOME\"}", now);
        welcomeMsg.setModel("rule-based-v1");
        messageRepository.save(welcomeMsg);
        return s;
    }

    private ChatbotMessage persistMessage(Integer sessionId, String sender, String content,
                                          String meta, OffsetDateTime when) {
        ChatbotMessage m = new ChatbotMessage();
        m.setSessionId(sessionId);
        m.setSender(sender);
        m.setContent(content);
        m.setMeta(meta != null ? meta : "{}");
        m.setCreatedAt(when);
        return messageRepository.save(m);
    }

    private ChatbotSession requireOwned(int accountId, int sessionId) {
        ChatbotSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy phiên chatbot"));
        if (s.getAccountId() == null || s.getAccountId() != accountId) {
            throw new ApiException(403, "Bạn không có quyền truy cập phiên này");
        }
        return s;
    }

    private ChatbotMessageDto toDto(ChatbotMessage m) {
        long createdMs = m.getCreatedAt() != null ? m.getCreatedAt().toInstant().toEpochMilli() : 0L;
        // Suggestions chỉ áp cho assistant; user message không có
        if ("assistant".equalsIgnoreCase(m.getSender())) {
            String intent = parseIntentFromMeta(m.getMeta());
            // Khi load lại lịch sử cũ, không tái tạo suggestions — để client có thể quyết định
            // có hiển thị quick replies không (chỉ cho message mới nhất).
            return new ChatbotMessageDto(m.getId(), m.getSender(), m.getContent(),
                    intent, Collections.emptyList(), createdMs);
        }
        return new ChatbotMessageDto(m.getId(), m.getSender(), m.getContent(),
                null, null, createdMs);
    }

    private ChatbotMessageDto toAssistantDto(ChatbotMessage m, String intent, List<String> suggestions) {
        long createdMs = m.getCreatedAt() != null ? m.getCreatedAt().toInstant().toEpochMilli() : 0L;
        return new ChatbotMessageDto(m.getId(), m.getSender(), m.getContent(),
                intent, suggestions, createdMs);
    }

    private String parseIntentFromMeta(String meta) {
        if (meta == null) return null;
        int idx = meta.indexOf("\"intent\":\"");
        if (idx < 0) return null;
        int start = idx + "\"intent\":\"".length();
        int end = meta.indexOf("\"", start);
        return end > start ? meta.substring(start, end) : null;
    }
}
