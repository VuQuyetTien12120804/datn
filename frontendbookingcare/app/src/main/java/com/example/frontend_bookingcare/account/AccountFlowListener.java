package com.example.frontend_bookingcare.account;

public interface AccountFlowListener {
    void openLogin();

    void openForgotPassword();

    void openForgotPasswordOtp();

    void openForgotPasswordNewPassword();

    void openRegister();

    void openVerifyOtp();

    void openChangePassword();

    void openEditProfile();

    void openProfileDetail();

    void onLoggedIn();

    void onRegisterComplete();
}
