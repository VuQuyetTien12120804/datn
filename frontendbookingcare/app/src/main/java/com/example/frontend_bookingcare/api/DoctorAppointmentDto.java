package com.example.frontend_bookingcare.api;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * Một dòng lịch trong panel bác sĩ (PENDING / CONFIRMED / CANCELLED / …).
 * Một số field backend có thể là chuỗi hoặc mảng (vd. {@code dob}, {@code expectedTime}) — giữ {@link JsonElement}.
 */
public class DoctorAppointmentDto {

    @SerializedName("appointmentId")
    public Integer appointmentId;

    @SerializedName(value = "patientId", alternate = {"patient_id"})
    public Integer patientId;

    @SerializedName("patientName")
    public String patientName;

    @SerializedName("phoneNumber")
    public String phoneNumber;

    @SerializedName("phone")
    public String phone;

    @SerializedName("email")
    public String email;

    /** Ngày sinh — backend có thể trả chuỗi, mảng [y,m,d], object, hoặc tên field khác. */
    @SerializedName(value = "dob", alternate = {
            "dateOfBirth", "birthDate", "patientDob", "date_of_birth", "birth_date"
    })
    public JsonElement dob;

    /** Một số API trả sẵn tuổi; ưu tiên khi parse dob thất bại. */
    @SerializedName(value = "patientAge", alternate = {"age", "yearsOld"})
    public Integer patientAge;

    @SerializedName("notes")
    public String notes;

    @SerializedName("reason")
    public String reason;

    @SerializedName(value = "clinicalNote", alternate = {"note"})
    public String clinicalNote;

    @SerializedName("address")
    public String address;

    @SerializedName("appointmentDate")
    public String appointmentDate;

    @SerializedName("expectedTime")
    public JsonElement expectedTime;

    @SerializedName("status")
    public String status;

    @SerializedName("gender")
    public String gender;
}
