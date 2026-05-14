package com.example.frontend_bookingcare.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("api/v1/auth/login")
    Call<ApiEnvelope> login(@Body AuthRequest body);

    @POST("api/v1/auth/refresh")
    Call<ApiEnvelope> refresh(@Body RefreshRequest body);

    @POST("api/v1/auth/register")
    Call<ApiEnvelope> register(@Body RegisterRequest body);

    @POST("api/v1/auth/otp/email/request")
    Call<ApiEnvelope> requestEmailOtp(@Body RequestEmailOtpRequest body);

    @POST("api/v1/auth/otp/email/verify")
    Call<ApiEnvelope> verifyEmailOtp(@Body VerifyEmailOtpRequest body);

    @POST("api/v1/auth/password-reset/request-otp")
    Call<ApiEnvelope> requestPasswordResetOtp(@Body RequestEmailOtpRequest body);

    @POST("api/v1/auth/password-reset/confirm")
    Call<ApiEnvelope> confirmPasswordReset(@Body PasswordResetConfirmRequest body);

    @POST("api/v1/auth/change-password")
    Call<ApiEnvelope> changePassword(
            @Header("Authorization") String authorization,
            @Body ChangePasswordRequest body
    );

    @POST("api/v1/auth/logout")
    Call<ApiEnvelope> logout(
            @Header("Authorization") String authorization,
            @Body LogoutRequest body
    );
}
