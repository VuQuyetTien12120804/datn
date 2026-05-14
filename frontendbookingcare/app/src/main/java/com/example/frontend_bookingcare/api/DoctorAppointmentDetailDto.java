package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

/**
 * Chi tiết lịch hẹn cho bác sĩ (GET /api/v1/doctors/appointment/{id}/detail).
 * Cấu trúc tương thích {@code AppointmentAdminDetail} trên web-admin.
 */
public class DoctorAppointmentDetailDto {

    @SerializedName("appointmentId")
    public Integer appointmentId;

    @SerializedName(value = "patientDob", alternate = {"dob", "dateOfBirth", "birthDate"})
    public String patientDob;

    @SerializedName(value = "patientGender", alternate = {"gender"})
    public String patientGender;
}
