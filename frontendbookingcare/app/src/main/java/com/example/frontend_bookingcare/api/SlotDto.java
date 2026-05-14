package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

/** Khung giờ khám của 1 bác sĩ trong 1 ngày — tương ứng MedicalScheduleSlotResponse ở backend. */
public class SlotDto {

    @SerializedName("doctorId")
    public Integer doctorId;

    @SerializedName("slotId")
    public Integer slotId;

    /** Ngày khám — backend trả "yyyy-MM-dd". */
    @SerializedName("slotDate")
    public String slotDate;

    /** Giờ bắt đầu — "HH:mm:ss" hoặc "HH:mm". */
    @SerializedName("startTime")
    public String startTime;

    @SerializedName("endTime")
    public String endTime;

    @SerializedName("isAvailable")
    public Boolean isAvailable;
}
