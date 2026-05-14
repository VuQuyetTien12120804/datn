package com.example.frontend_bookingcare.ui.booking;

import android.content.Intent;

import androidx.annotation.Nullable;

/**
 * Gói dữ liệu truyền giữa 3 màn đặt lịch. Serialize thành Intent extras — đơn giản
 * hơn Parcelable/Serializable cho trường hợp này.
 *
 *  Bước 1 (Step1Activity):  nhập doctorId/name → chọn date + slot → sang Step2
 *  Bước 2 (Step2Activity):  review → bấm Xác nhận → POST /book → sang Step3 (Result)
 *  Bước 3 (ResultActivity): chỉ hiển thị, không cần draft nữa (dùng BookingResponse).
 */
public class BookingDraft {

    private static final String KEY_DOCTOR_ID = "bd_doctor_id";
    private static final String KEY_DOCTOR_TITLE = "bd_doctor_title";
    private static final String KEY_DOCTOR_NAME = "bd_doctor_name";
    private static final String KEY_DOCTOR_SPECIALTY = "bd_doctor_specialty";
    private static final String KEY_DOCTOR_AVATAR_BG = "bd_doctor_avatar_bg";
    private static final String KEY_SLOT_ID = "bd_slot_id";
    private static final String KEY_SLOT_DATE = "bd_slot_date";
    private static final String KEY_START_TIME = "bd_start_time";
    private static final String KEY_END_TIME = "bd_end_time";
    private static final String KEY_SESSION = "bd_session";
    private static final String KEY_NOTES = "bd_notes";
    private static final String KEY_FULL_NAME = "bd_full_name";
    private static final String KEY_EMAIL = "bd_email";
    private static final String KEY_PHONE = "bd_phone";
    private static final String KEY_DOB = "bd_dob";
    private static final String KEY_GENDER = "bd_gender";
    private static final String KEY_ADDRESS = "bd_address";

    public int doctorId;
    @Nullable public String doctorTitle;
    @Nullable public String doctorName;
    @Nullable public String doctorSpecialty;
    public int doctorAvatarBg;

    public int slotId;
    /** "yyyy-MM-dd". */
    @Nullable public String slotDate;
    /** "HH:mm". */
    @Nullable public String startTime;
    @Nullable public String endTime;
    /** "morning" | "afternoon" — để Step2 hiện đúng "Buổi sáng / Buổi chiều". */
    @Nullable public String session;

    @Nullable public String notes;

    // Thông tin bệnh nhân lấy từ SessionManager + ProfileExtras.
    @Nullable public String fullName;
    @Nullable public String email;
    @Nullable public String phone;
    /** "dd/MM/yyyy" từ ProfileExtras (chưa chuẩn hoá). */
    @Nullable public String dob;
    /** Raw text từ profile — "Nam"/"Nữ"/"Khác" hoặc english. */
    @Nullable public String gender;
    @Nullable public String address;

    public void writeTo(Intent i) {
        i.putExtra(KEY_DOCTOR_ID, doctorId);
        i.putExtra(KEY_DOCTOR_TITLE, doctorTitle);
        i.putExtra(KEY_DOCTOR_NAME, doctorName);
        i.putExtra(KEY_DOCTOR_SPECIALTY, doctorSpecialty);
        i.putExtra(KEY_DOCTOR_AVATAR_BG, doctorAvatarBg);
        i.putExtra(KEY_SLOT_ID, slotId);
        i.putExtra(KEY_SLOT_DATE, slotDate);
        i.putExtra(KEY_START_TIME, startTime);
        i.putExtra(KEY_END_TIME, endTime);
        i.putExtra(KEY_SESSION, session);
        i.putExtra(KEY_NOTES, notes);
        i.putExtra(KEY_FULL_NAME, fullName);
        i.putExtra(KEY_EMAIL, email);
        i.putExtra(KEY_PHONE, phone);
        i.putExtra(KEY_DOB, dob);
        i.putExtra(KEY_GENDER, gender);
        i.putExtra(KEY_ADDRESS, address);
    }

    public static BookingDraft readFrom(Intent i) {
        BookingDraft d = new BookingDraft();
        d.doctorId = i.getIntExtra(KEY_DOCTOR_ID, -1);
        d.doctorTitle = i.getStringExtra(KEY_DOCTOR_TITLE);
        d.doctorName = i.getStringExtra(KEY_DOCTOR_NAME);
        d.doctorSpecialty = i.getStringExtra(KEY_DOCTOR_SPECIALTY);
        d.doctorAvatarBg = i.getIntExtra(KEY_DOCTOR_AVATAR_BG, 0);
        d.slotId = i.getIntExtra(KEY_SLOT_ID, -1);
        d.slotDate = i.getStringExtra(KEY_SLOT_DATE);
        d.startTime = i.getStringExtra(KEY_START_TIME);
        d.endTime = i.getStringExtra(KEY_END_TIME);
        d.session = i.getStringExtra(KEY_SESSION);
        d.notes = i.getStringExtra(KEY_NOTES);
        d.fullName = i.getStringExtra(KEY_FULL_NAME);
        d.email = i.getStringExtra(KEY_EMAIL);
        d.phone = i.getStringExtra(KEY_PHONE);
        d.dob = i.getStringExtra(KEY_DOB);
        d.gender = i.getStringExtra(KEY_GENDER);
        d.address = i.getStringExtra(KEY_ADDRESS);
        return d;
    }
}
