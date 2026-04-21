package com.example.frontend_bookingcare.api;

public class PasswordResetConfirmRequest {
    public String email;
    public String otp;
    public String newPassword;

    public PasswordResetConfirmRequest(String email, String otp, String newPassword) {
        this.email = email;
        this.otp = otp;
        this.newPassword = newPassword;
    }
}
