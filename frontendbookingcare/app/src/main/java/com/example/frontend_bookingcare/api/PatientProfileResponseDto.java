package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

public class PatientProfileResponseDto {
    @SerializedName("patientId")
    public Integer patientId;

    @SerializedName("fullName")
    public String fullName;

    @SerializedName("email")
    public String email;

    @SerializedName("phone")
    public String phone;

    @SerializedName("dob")
    public String dob; // dd/MM/yyyy

    @SerializedName("gender")
    public String gender;

    @SerializedName("address")
    public String address;
}

