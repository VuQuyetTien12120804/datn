package com.bookingcare.backend_bookingcare.controller;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.dto.AuthResponseDto;
import com.bookingcare.backend_bookingcare.dto.LoginRequest;
import com.bookingcare.backend_bookingcare.dto.RefreshRequest;
import com.bookingcare.backend_bookingcare.security.CurrentAccountService;
import com.bookingcare.backend_bookingcare.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentAccountService currentAccountService;

    @PostMapping("/login")
    public ApiEnvelope<AuthResponseDto> login(@Valid @RequestBody LoginRequest request) {
        return ApiEnvelope.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiEnvelope<AuthResponseDto> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiEnvelope.ok(authService.refresh(request));
    }

    @PostMapping("/register")
    public ApiEnvelope<AuthResponseDto> register(@Valid @RequestBody RegisterBody body) {
        return ApiEnvelope.ok(authService.register(body.email, body.password, body.fullName, body.phone));
    }

    @PostMapping("/otp/email/request")
    public ApiEnvelope<Map<String, Object>> requestOtp(@Valid @RequestBody EmailBody body) {
        return ApiEnvelope.ok(authService.requestEmailOtp(body.email, "REGISTER"));
    }

    @PostMapping("/otp/email/verify")
    public ApiEnvelope<Void> verifyOtp(@RequestBody OtpBody body) {
        authService.verifyEmailOtp(body.email, body.otp);
        return ApiEnvelope.ok(null);
    }

    @PostMapping("/password-reset/request-otp")
    public ApiEnvelope<Map<String, Object>> resetRequest(@Valid @RequestBody EmailBody body) {
        return ApiEnvelope.ok(authService.requestPasswordResetOtp(body.email));
    }

    @PostMapping("/password-reset/confirm")
    public ApiEnvelope<Void> resetConfirm(@RequestBody ResetConfirmBody body) {
        authService.confirmPasswordReset(body.email, body.otp, body.newPassword);
        return ApiEnvelope.ok(null);
    }

    @PostMapping("/change-password")
    public ApiEnvelope<Void> changePassword(
            @RequestHeader(value = "Authorization", required = false) String ignored,
            @RequestBody ChangePasswordBody body
    ) {
        authService.changePassword(
                currentAccountService.requireUser().getAccountId(),
                body.oldPassword,
                body.newPassword
        );
        return ApiEnvelope.ok(null);
    }

    @PostMapping("/logout")
    public ApiEnvelope<Void> logout() {
        authService.logout();
        return ApiEnvelope.ok(null);
    }

    @Getter
    @Setter
    public static class RegisterBody {
        @NotBlank
        @Email
        private String email;
        @NotBlank
        @Size(min = 6)
        private String password;
        @NotBlank
        private String fullName;
        private String phone;
    }

    @Getter
    @Setter
    public static class EmailBody {
        @NotBlank
        @Email
        private String email;
    }

    @Getter
    @Setter
    public static class OtpBody {
        private String email;
        private String otp;
    }

    @Getter
    @Setter
    public static class ResetConfirmBody {
        private String email;
        private String otp;
        private String newPassword;
    }

    @Getter
    @Setter
    public static class ChangePasswordBody {
        private String oldPassword;
        private String newPassword;
    }
}
