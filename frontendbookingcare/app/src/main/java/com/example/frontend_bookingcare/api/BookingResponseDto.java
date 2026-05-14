package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

/** Data trả về sau khi đặt lịch thành công — tương ứng AppointmentResponse. */
public class BookingResponseDto {

    @SerializedName("appointmentId")
    public Integer appointmentId;

    @SerializedName("doctorId")
    public Integer doctorId;

    @SerializedName("appointmentDate")
    public String appointmentDate;

    @SerializedName("startTime")
    public String startTime;

    @SerializedName("endTime")
    public String endTime;

    @SerializedName("status")
    public String status;

    @SerializedName("notes")
    public String notes;

    @SerializedName("createdAt")
    public String createdAt;
}
