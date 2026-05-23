package com.bookingcare.backend_bookingcare.controller;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.dto.ClinicalNoteRequest;
import com.bookingcare.backend_bookingcare.security.CurrentAccountService;
import com.bookingcare.backend_bookingcare.service.DoctorPanelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorPanelController {

    private final DoctorPanelService doctorPanelService;
    private final CurrentAccountService currentAccountService;

    @GetMapping("/profile")
    public ApiEnvelope<Map<String, Object>> profile() {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(doctorPanelService.profile(accountId));
    }

    @GetMapping("/appointment-status")
    public ApiEnvelope<List<Map<String, Object>>> byStatus(@RequestParam String status) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(doctorPanelService.byStatus(accountId, status));
    }

    @GetMapping("/appointment-today")
    public ApiEnvelope<List<Map<String, Object>>> today() {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(doctorPanelService.today(accountId));
    }

    @GetMapping("/appointment/{appointmentId}/detail")
    public ApiEnvelope<Map<String, Object>> detail(@PathVariable int appointmentId) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(doctorPanelService.detail(accountId, appointmentId));
    }

    @PatchMapping("/appointment/{appointmentId}/confirm")
    public ApiEnvelope<Map<String, Object>> confirm(@PathVariable int appointmentId) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(doctorPanelService.confirm(accountId, appointmentId));
    }

    @PatchMapping("/appointment/{appointmentId}/cancel")
    public ApiEnvelope<Map<String, Object>> cancel(@PathVariable int appointmentId) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(doctorPanelService.cancel(accountId, appointmentId));
    }

    @PatchMapping("/appointment/{appointmentId}/start")
    public ApiEnvelope<Map<String, Object>> startExam(@PathVariable int appointmentId) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(doctorPanelService.startExam(accountId, appointmentId));
    }

    @PatchMapping("/appointment/{appointmentId}/note")
    public ApiEnvelope<Map<String, Object>> saveNote(
            @PathVariable int appointmentId,
            @RequestBody ClinicalNoteRequest body
    ) {
        int accountId = currentAccountService.requireUser().getAccountId();
        String note = body != null ? body.getNote() : null;
        return ApiEnvelope.ok(doctorPanelService.saveClinicalNote(accountId, appointmentId, note));
    }

    @PatchMapping("/appointment/{appointmentId}/complete")
    public ApiEnvelope<Map<String, Object>> completeExam(
            @PathVariable int appointmentId,
            @RequestBody(required = false) ClinicalNoteRequest body
    ) {
        int accountId = currentAccountService.requireUser().getAccountId();
        String note = body != null ? body.getNote() : null;
        return ApiEnvelope.ok(doctorPanelService.completeExam(accountId, appointmentId, note));
    }

    @PatchMapping("/appointment/{appointmentId}/no-show")
    public ApiEnvelope<Map<String, Object>> markNoShow(@PathVariable int appointmentId) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(doctorPanelService.markNoShow(accountId, appointmentId));
    }
}
