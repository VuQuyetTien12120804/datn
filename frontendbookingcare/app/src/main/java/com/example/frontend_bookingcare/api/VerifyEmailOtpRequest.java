package com.example.frontend_bookingcare.api;

public class VerifyEmailOtpRequest {
    public String email;
    public String otp;

    public VerifyEmailOtpRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }
}
