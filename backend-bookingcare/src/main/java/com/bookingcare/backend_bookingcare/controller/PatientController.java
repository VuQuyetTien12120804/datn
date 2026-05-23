package com.bookingcare.backend_bookingcare.controller;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.security.CurrentAccountService;
import com.bookingcare.backend_bookingcare.service.PatientService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final CurrentAccountService currentAccountService;

    @GetMapping("/profile")
    public ApiEnvelope<Map<String, Object>> profile() {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(patientService.getProfile(accountId));
    }

    @PutMapping("/profile")
    public ApiEnvelope<Map<String, Object>> updateProfile(@RequestBody UpdateProfileBody body) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(patientService.updateProfile(
                accountId, body.phone, body.dob, body.gender, body.address));
    }

    @GetMapping("/appointments")
    public ApiEnvelope<List<Map<String, Object>>> appointments(
            @RequestParam(defaultValue = "UPCOMING") String group
    ) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(patientService.listAppointments(accountId, group));
    }

    @GetMapping("/appointments/{appointmentId}")
    public ApiEnvelope<Map<String, Object>> appointmentDetail(@PathVariable int appointmentId) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(patientService.getAppointmentDetail(accountId, appointmentId));
    }

    @PostMapping("/appointments/{appointmentId}/cancel")
    public ApiEnvelope<Void> cancel(
            @PathVariable int appointmentId,
            @RequestBody(required = false) CancelBody body
    ) {
        int accountId = currentAccountService.requireUser().getAccountId();
        String reason = body != null ? body.cancelReason : null;
        patientService.cancelAppointment(accountId, appointmentId, reason);
        return ApiEnvelope.ok(null);
    }

    @PostMapping("/appointments/{appointmentId}/check-in")
    public ApiEnvelope<Map<String, Object>> checkIn(@PathVariable int appointmentId) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(patientService.checkInAppointment(accountId, appointmentId));
    }

    @Getter
    @Setter
    public static class UpdateProfileBody {
        private String phone;
        private String dob;
        private String gender;
        private String address;
    }

    @Getter
    @Setter
    public static class CancelBody {
        private String cancelReason;
    }
}
