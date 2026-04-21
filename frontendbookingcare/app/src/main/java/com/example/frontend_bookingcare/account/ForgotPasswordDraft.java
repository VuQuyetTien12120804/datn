package com.example.frontend_bookingcare.account;

/** Luồng quên mật khẩu: email → OTP → mật khẩu mới. */
public class ForgotPasswordDraft {
    public String email = "";
    public String otp = "";

    public void clear() {
        email = "";
        otp = "";
    }
}
