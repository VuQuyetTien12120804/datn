package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

public class SpecialtyDto {

    @SerializedName("specialtyId")
    public Integer specialtyId;

    @SerializedName("code")
    public String code;

    @SerializedName("name")
    public String name;

    @SerializedName("description")
    public String description;
}
