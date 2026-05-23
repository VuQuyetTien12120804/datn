package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.config.MailProperties;
import com.bookingcare.backend_bookingcare.dto.AuthResponseDto;
import com.bookingcare.backend_bookingcare.dto.LoginRequest;
import com.bookingcare.backend_bookingcare.dto.RefreshRequest;
import com.bookingcare.backend_bookingcare.entity.Account;
import com.bookingcare.backend_bookingcare.entity.EmailVerificationOtp;
import com.bookingcare.backend_bookingcare.entity.Patient;
import com.bookingcare.backend_bookingcare.entity.Role;
import com.bookingcare.backend_bookingcare.repository.AccountRepository;
import com.bookingcare.backend_bookingcare.repository.EmailVerificationOtpRepository;
import com.bookingcare.backend_bookingcare.repository.PatientRepository;
import com.bookingcare.backend_bookingcare.repository.RoleRepository;
import com.bookingcare.backend_bookingcare.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String PURPOSE_REGISTER = "REGISTER";
    private static final String PURPOSE_RESET = "RESET";

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final EmailVerificationOtpRepository otpRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final MailProperties mailProperties;

    @Transactional
    public AuthResponseDto login(LoginRequest request) {
        Account account = findByEmailOrThrow(request.getEmail());
        assertPassword(account, request.getPassword());
        assertActive(account);
        account.setLastLoginAt(OffsetDateTime.now());
        accountRepository.save(account);
        return buildTokenPair(account);
    }

    @Transactional(readOnly = true)
    public AuthResponseDto refresh(RefreshRequest request) {
        try {
            Claims claims = jwtService.parse(request.getRefreshToken());
            if (!"refresh".equals(claims.get("type", String.class))) {
                throw new ApiException(401, "Refresh token không hợp lệ");
            }
            int accountId = Integer.parseInt(claims.getSubject());
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ApiException(401, "Refresh token không hợp lệ"));
            assertActive(account);
            return buildTokenPair(account);
        } catch (JwtException ex) {
            throw new ApiException(401, "Refresh token không hợp lệ");
        }
    }

    @Transactional
    public AuthResponseDto register(String email, String password, String fullName, String phone) {
        String em = normalizeEmail(email);
        if (em.isBlank()) {
            throw new ApiException(400, "Email không hợp lệ");
        }
        if (password == null || password.length() < 6) {
            throw new ApiException(400, "Mật khẩu phải có ít nhất 6 ký tự");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new ApiException(400, "Họ tên không được để trống");
        }
        assertRegisterOtpVerified(em);
        if (accountRepository.findByEmailIgnoreCase(em).isPresent()) {
            throw new ApiException(409, "Email đã được đăng ký");
        }
        Role patientRole = roleRepository.findByCodeIgnoreCase("patient")
                .orElseThrow(() -> new ApiException(500, "Role patient chưa cấu hình"));

        Account account = new Account();
        account.setRole(patientRole);
        account.setEmail(em);
        account.setPhone(phone);
        account.setPassword(password);
        account.setFullName(fullName != null ? fullName.trim() : "Bệnh nhân");
        account.setStatus("active");
        account.setEmailVerified(true);
        account.setCreatedAt(OffsetDateTime.now());
        account.setUpdatedAt(OffsetDateTime.now());
        account = accountRepository.save(account);

        Patient patient = new Patient();
        patient.setAccountId(account.getId());
        patient.setFullName(account.getFullName());
        patient.setEmail(account.getEmail());
        patient.setPhone(account.getPhone());
        patient.setGender("unknown");
        patient.setCreatedAt(OffsetDateTime.now());
        patient.setUpdatedAt(OffsetDateTime.now());
        patientRepository.save(patient);

        return buildTokenPair(account);
    }

    @Transactional
    public Map<String, Object> requestEmailOtp(String email, String purpose) {
        String em = normalizeEmail(email);
        String resolvedPurpose = purpose != null ? purpose : PURPOSE_REGISTER;
        if (PURPOSE_REGISTER.equals(resolvedPurpose)
                && accountRepository.findByEmailIgnoreCase(em).isPresent()) {
            throw new ApiException(409, "Email đã được đăng ký");
        }

        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        EmailVerificationOtp row = new EmailVerificationOtp();
        row.setEmail(em);
        row.setPurpose(resolvedPurpose);
        row.setOtpHash(otp);
        row.setExpiresAt(OffsetDateTime.now().plusMinutes(15));
        row.setCreatedAt(OffsetDateTime.now());
        otpRepository.save(row);

        if (mailProperties.isExposeOtpInResponse()) {
            Map<String, Object> dev = new HashMap<>();
            dev.put("otp", otp);
            dev.put("expiresAt", row.getExpiresAt().toString());
            return dev;
        }

        emailService.sendOtp(em, otp, resolvedPurpose);

        Map<String, Object> out = new HashMap<>();
        out.put("expiresAt", row.getExpiresAt().toString());
        out.put("message", "OTP đã được gửi tới email");
        return out;
    }

    @Transactional
    public void verifyEmailOtp(String email, String otp) {
        consumeOtp(normalizeEmail(email), PURPOSE_REGISTER, otp);
        accountRepository.findByEmailIgnoreCase(normalizeEmail(email)).ifPresent(acc -> {
            acc.setEmailVerified(true);
            accountRepository.save(acc);
        });
    }

    @Transactional
    public Map<String, Object> requestPasswordResetOtp(String email) {
        String em = normalizeEmail(email);
        if (accountRepository.findByEmailIgnoreCase(em).isEmpty()) {
            throw new ApiException(404, "Email không tồn tại");
        }
        return requestEmailOtp(em, PURPOSE_RESET);
    }

    @Transactional
    public void confirmPasswordReset(String email, String otp, String newPassword) {
        String em = normalizeEmail(email);
        if (newPassword == null || newPassword.length() < 6) {
            throw new ApiException(400, "Mật khẩu phải có ít nhất 6 ký tự");
        }
        consumeOtp(em, PURPOSE_RESET, otp);
        Account account = findByEmailOrThrow(em);
        account.setPassword(newPassword);
        account.setUpdatedAt(OffsetDateTime.now());
        accountRepository.save(account);
    }

    @Transactional
    public void changePassword(int accountId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new ApiException(400, "Mật khẩu phải có ít nhất 6 ký tự");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy tài khoản"));
        assertPassword(account, oldPassword);
        account.setPassword(newPassword);
        account.setUpdatedAt(OffsetDateTime.now());
        accountRepository.save(account);
    }

    public void logout() {
        // Stateless JWT — client xóa token là đủ.
    }

    private void consumeOtp(String email, String purpose, String otp) {
        EmailVerificationOtp row = otpRepository
                .findTopByEmailIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new ApiException(400, "OTP không hợp lệ"));
        if (row.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(400, "OTP đã hết hạn");
        }
        if (!row.getOtpHash().equals(otp)) {
            throw new ApiException(400, "OTP không đúng");
        }
        row.setConsumedAt(OffsetDateTime.now());
        otpRepository.save(row);
    }

    /** Đăng ký chỉ được phép sau khi OTP REGISTER đã verify (trong vòng 30 phút). */
    private void assertRegisterOtpVerified(String email) {
        EmailVerificationOtp verified = otpRepository
                .findTopByEmailIgnoreCaseAndPurposeAndConsumedAtIsNotNullOrderByConsumedAtDesc(
                        email, PURPOSE_REGISTER)
                .orElseThrow(() -> new ApiException(400, "Email chưa được xác thực OTP"));
        if (verified.getConsumedAt().isBefore(OffsetDateTime.now().minusMinutes(30))) {
            throw new ApiException(400, "OTP xác thực đã hết hạn, vui lòng xác thực lại");
        }
    }

    private Account findByEmailOrThrow(String email) {
        return accountRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ApiException(401, "Email hoặc mật khẩu không đúng"));
    }

    private void assertPassword(Account account, String password) {
        if (account.getPassword() == null || !account.getPassword().equals(password)) {
            throw new ApiException(401, "Email hoặc mật khẩu không đúng");
        }
    }

    private void assertActive(Account account) {
        if (!"active".equalsIgnoreCase(account.getStatus())) {
            throw new ApiException(403, "Tài khoản không hoạt động");
        }
        if (account.getRole() == null || Boolean.TRUE.equals(account.getRole().getDeleted())) {
            throw new ApiException(403, "Vai trò không hợp lệ");
        }
    }

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    private AuthResponseDto buildTokenPair(Account account) {
        String role = account.getRole().getCode();
        String access = jwtService.createAccessToken(account.getId(), role, JwtService.newJti());
        String refresh = jwtService.createRefreshToken(account.getId(), JwtService.newJti());
        return AuthResponseDto.builder()
                .userId(String.valueOf(account.getId()))
                .accessToken(access)
                .refreshToken(refresh)
                .role(role)
                .fullName(account.getFullName())
                .email(account.getEmail())
                .build();
    }
}
