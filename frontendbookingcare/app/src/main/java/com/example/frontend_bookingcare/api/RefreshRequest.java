package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

/** Body cho POST /api/v1/auth/refresh. */
public class RefreshRequest {

    @SerializedName("refreshToken")
    public String refreshToken;

    public RefreshRequest() {
    }

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
