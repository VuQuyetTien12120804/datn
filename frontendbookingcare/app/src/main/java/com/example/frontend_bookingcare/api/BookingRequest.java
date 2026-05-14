package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

/** Body cho POST /api/v1/booking/book — khớp AppointmentRequest ở backend. */
public class BookingRequest {

    @SerializedName("doctorId")
    public Integer doctorId;

    @SerializedName("slotId")
    public Integer slotId;

    /** "yyyy-MM-dd". */
    @SerializedName("appointmentDate")
    public String appointmentDate;

    @SerializedName("fullName")
    public String fullName;

    @SerializedName("email")
    public String email;

    @SerializedName("phoneNumber")
    public String phoneNumber;

    /** "yyyy-MM-dd" — có thể null nếu user chưa cập nhật ngày sinh. */
    @SerializedName("dob")
    public String dob;

    @SerializedName("address")
    public String address;

    /** "MALE" | "FEMALE" | "OTHER". */
    @SerializedName("gender")
    public String gender;

    @SerializedName("notes")
    public String notes;
}
