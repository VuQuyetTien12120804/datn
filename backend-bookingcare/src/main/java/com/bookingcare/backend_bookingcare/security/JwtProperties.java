package com.bookingcare.backend_bookingcare.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessExpirationMinutes,
        long refreshExpirationDays
) {
}
