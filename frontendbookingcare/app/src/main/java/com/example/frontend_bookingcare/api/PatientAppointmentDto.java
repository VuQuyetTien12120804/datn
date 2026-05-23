package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

public class PatientAppointmentDto {
    @SerializedName("appointmentId")
    public Integer appointmentId;

    @SerializedName("doctorId")
    public Integer doctorId;

    @SerializedName("doctorName")
    public String doctorName;

    @SerializedName("specialty")
    public String specialty;

    @SerializedName("appointmentDate")
    public String appointmentDate; // yyyy-MM-dd

    @SerializedName("startTime")
    public String startTime; // HH:mm

    @SerializedName("endTime")
    public String endTime; // HH:mm

    @SerializedName("status")
    public String status;

    @SerializedName("clinicalNote")
    public String clinicalNote;

    @SerializedName("reason")
    public String reason;

    @SerializedName("queueNumber")
    public Integer queueNumber;
}

