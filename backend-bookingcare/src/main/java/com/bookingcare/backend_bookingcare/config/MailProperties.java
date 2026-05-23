package com.bookingcare.backend_bookingcare.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** Địa chỉ hiển thị người gửi (thường trùng spring.mail.username). */
    private String from = "";

    private String fromName = "BookingCare";

    /** Chỉ bật khi dev — trả OTP trong JSON response (không gửi email). */
    private boolean exposeOtpInResponse = false;
}
