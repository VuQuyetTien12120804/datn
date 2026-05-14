package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

public class CancelAppointmentRequest {
    @SerializedName("cancelReason")
    public String cancelReason;

    public CancelAppointmentRequest(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}

