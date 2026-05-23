package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

public class DoctorProfileDto {
    @SerializedName("doctorId")
    public Integer doctorId;

    @SerializedName("fullName")
    public String fullName;

    @SerializedName("email")
    public String email;

    @SerializedName("phone")
    public String phone;

    @SerializedName("licenseNo")
    public String licenseNo;

    @SerializedName("bio")
    public String bio;

    @SerializedName("roomLocation")
    public String roomLocation;

    @SerializedName("scheduleText")
    public String scheduleText;

    @SerializedName("specialty")
    public String specialty;

    @SerializedName("clinicName")
    public String clinicName;
}
