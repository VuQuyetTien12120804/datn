package com.example.frontend_bookingcare.api;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * Chi tiết lịch hẹn cho bác sĩ (GET /api/v1/doctors/appointment/{id}/detail).
 */
public class DoctorAppointmentDetailDto {

    @SerializedName("appointmentId")
    public Integer appointmentId;

    @SerializedName("status")
    public String status;

    @SerializedName("patientName")
    public String patientName;

    @SerializedName("patientPhone")
    public String patientPhone;

    @SerializedName(value = "patientGender", alternate = {"gender"})
    public String patientGender;

    @SerializedName(value = "patientDob", alternate = {"dob", "dateOfBirth", "birthDate"})
    public String patientDob;

    @SerializedName("patientAddress")
    public String patientAddress;

    @SerializedName("reason")
    public String reason;

    @SerializedName(value = "note", alternate = {"clinicalNote"})
    public String note;

    @SerializedName("appointmentDate")
    public String appointmentDate;

    @SerializedName("expectedTime")
    public JsonElement expectedTime;

    @SerializedName("startsAt")
    public String startsAt;
}
