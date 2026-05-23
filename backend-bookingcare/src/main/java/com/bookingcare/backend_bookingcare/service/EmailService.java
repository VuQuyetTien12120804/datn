package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    /** Gửi mã OTP 6 số tới email người dùng. */
    public void sendOtp(String toEmail, String otp, String purpose) {
        if (!StringUtils.hasText(smtpUsername)) {
            throw new ApiException(500, "Chưa cấu hình email SMTP (spring.mail.username)");
        }
        if (!StringUtils.hasText(mailProperties.getFrom())) {
            throw new ApiException(500, "Chưa cấu hình app.mail.from");
        }

        String subject = buildSubject(purpose);
        String html = buildHtmlBody(otp, purpose);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent OTP email to {} purpose={}", toEmail, purpose);
        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}: {}", toEmail, ex.getMessage(), ex);
            throw new ApiException(500, "Không gửi được email OTP. Kiểm tra cấu hình Gmail App Password.");
        }
    }

    private String buildSubject(String purpose) {
        if ("RESET".equalsIgnoreCase(purpose)) {
            return "BookingCare — Mã OTP đặt lại mật khẩu";
        }
        return "BookingCare — Mã OTP xác thực đăng ký";
    }

    private String buildHtmlBody(String otp, String purpose) {
        String action = "RESET".equalsIgnoreCase(purpose)
                ? "đặt lại mật khẩu"
                : "xác thực đăng ký tài khoản";
        return """
                <div style="font-family:Arial,sans-serif;line-height:1.6;color:#222">
                  <h2 style="color:#1565C0">BookingCare</h2>
                  <p>Mã OTP để %s của bạn là:</p>
                  <p style="font-size:28px;font-weight:bold;letter-spacing:6px;color:#1565C0">%s</p>
                  <p>Mã có hiệu lực <strong>15 phút</strong>. Không chia sẻ mã này với ai.</p>
                  <p style="color:#666;font-size:12px">Nếu bạn không yêu cầu mã này, hãy bỏ qua email.</p>
                </div>
                """.formatted(action, otp);
    }
}
