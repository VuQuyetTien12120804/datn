package com.example.frontend_bookingcare.api;

public class LogoutRequest {
    public String refreshToken;

    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
