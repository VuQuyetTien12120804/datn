package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

public class DoctorDto {

    @SerializedName("doctorId")
    public Integer doctorId;

    @SerializedName("accountId")
    public Integer accountId;

    @SerializedName("fullName")
    public String fullName;

    @SerializedName("specialty")
    public String specialty;

    @SerializedName("bio")
    public String bio;

    @SerializedName("roomLocation")
    public String roomLocation;

    @SerializedName("clinicAddress")
    public String clinicAddress;
}
