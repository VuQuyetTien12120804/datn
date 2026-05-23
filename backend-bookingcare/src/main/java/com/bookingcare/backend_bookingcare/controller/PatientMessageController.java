package com.bookingcare.backend_bookingcare.controller;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.security.CurrentAccountService;
import com.bookingcare.backend_bookingcare.service.MessagingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/patient/messages")
@RequiredArgsConstructor
public class PatientMessageController {

    private final MessagingService messagingService;
    private final CurrentAccountService currentAccountService;

    @GetMapping("/threads")
    public ApiEnvelope<List<Map<String, Object>>> threads() {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(messagingService.listPatientThreads(accountId));
    }

    @GetMapping("/threads/{threadKey}/messages")
    public ApiEnvelope<List<Map<String, Object>>> messages(@PathVariable String threadKey) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(messagingService.listMessages("patient", accountId, threadKey));
    }

    @PostMapping("/threads/{threadKey}/messages")
    public ApiEnvelope<Map<String, Object>> send(@PathVariable String threadKey, @RequestBody SendBody body) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(messagingService.sendPatientMessage(accountId, threadKey, body.content));
    }

    @PostMapping("/threads/{threadKey}/read")
    public ApiEnvelope<Void> markRead(@PathVariable String threadKey) {
        int accountId = currentAccountService.requireUser().getAccountId();
        messagingService.markRead("patient", accountId, threadKey);
        return ApiEnvelope.ok(null);
    }

    @Getter
    @Setter
    public static class SendBody {
        private String content;
    }
}
