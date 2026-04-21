package com.example.frontend_bookingcare.session;

public class AuthSession {
    public String userId;
    public String accessToken;
    public String refreshToken;
    public String role;
    public String fullName;
    public String email;

    public AuthSession() {
    }

    public AuthSession(String userId, String accessToken, String refreshToken, String role, String fullName, String email) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
    }
}
