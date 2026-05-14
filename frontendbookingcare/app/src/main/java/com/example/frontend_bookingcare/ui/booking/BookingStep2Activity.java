package com.example.frontend_bookingcare.ui.booking;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.BookingRequest;
import com.example.frontend_bookingcare.data.BookingRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.google.android.material.button.MaterialButton;

/**
 * Bước 2: hiển thị review toàn bộ thông tin đã chọn ở bước 1 + thông tin bệnh nhân,
 * xác nhận → POST /api/v1/booking/book → mở {@link BookingResultActivity}.
 */
public class BookingStep2Activity extends AppCompatActivity {

    private BookingDraft draft;
    @Nullable private ProgressDialog loadingDialog;
    private MaterialButton confirmBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_step2);

        applyTopInsetToHeader(findViewById(R.id.booking_header));
        BookingStepperHelper.bind(findViewById(R.id.booking_stepper), 2);

        draft = BookingDraft.readFrom(getIntent());

        ImageButton back = findViewById(R.id.booking_back);
        back.setOnClickListener(v -> finish());

        confirmBtn = findViewById(R.id.booking_confirm);
        confirmBtn.setOnClickListener(v -> submitBooking());

        bindDoctor();
        bindAppointment();
        bindPatient();
    }

    private void bindDoctor() {
        FrameLayout avatarBg = findViewById(R.id.booking_doctor_avatar_bg);
        TextView avatarLetter = findViewById(R.id.booking_doctor_avatar_letter);
        TextView title = findViewById(R.id.booking_doctor_title);
        TextView name = findViewById(R.id.booking_doctor_name);
        TextView specialty = findViewById(R.id.booking_doctor_specialty);

        avatarBg.setBackgroundResource(draft.doctorAvatarBg != 0
                ? draft.doctorAvatarBg : R.drawable.bg_tile_blue);
        avatarLetter.setText(firstLetter(draft.doctorName));

        if (!TextUtils.isEmpty(draft.doctorTitle)) {
            title.setVisibility(View.VISIBLE);
            title.setText(draft.doctorTitle);
        } else {
            title.setVisibility(View.GONE);
        }
        name.setText(draft.doctorName != null ? draft.doctorName : "");

        if (!TextUtils.isEmpty(draft.doctorSpecialty)) {
            specialty.setVisibility(View.VISIBLE);
            specialty.setText(getString(R.string.booking_specialty_fmt, draft.doctorSpecialty));
        } else {
            specialty.setVisibility(View.GONE);
        }
    }

    private void bindAppointment() {
        TextView time = findViewById(R.id.booking_summary_time);
        TextView session = findViewById(R.id.booking_summary_session);
        TextView date = findViewById(R.id.booking_summary_date);

        time.setText(BookingFormatters.timeRange(draft.startTime, draft.endTime));
        session.setText("morning".equals(draft.session)
                ? R.string.booking_session_morning : R.string.booking_session_afternoon);
        date.setText(BookingFormatters.prettyDate(draft.slotDate));
    }

    private void bindPatient() {
        TextView name = findViewById(R.id.booking_summary_name);
        TextView gender = findViewById(R.id.booking_summary_gender);
        TextView dob = findViewById(R.id.booking_summary_dob);
        TextView phone = findViewById(R.id.booking_summary_phone);
        TextView address = findViewById(R.id.booking_summary_address);

        name.setText(TextUtils.isEmpty(draft.fullName)
                ? getString(R.string.booking_not_available) : draft.fullName);
        gender.setText(BookingFormatters.displayGender(draft.gender));
        dob.setText(TextUtils.isEmpty(draft.dob) ? getString(R.string.booking_not_updated) : draft.dob);
        phone.setText(TextUtils.isEmpty(draft.phone) ? getString(R.string.booking_not_updated) : draft.phone);
        address.setText(TextUtils.isEmpty(draft.address)
                ? getString(R.string.booking_not_updated) : draft.address);
    }

    // ---------- Submit ----------

    private void submitBooking() {
        if (draft.doctorId <= 0 || draft.slotId <= 0 || TextUtils.isEmpty(draft.slotDate)) {
            Toast.makeText(this, R.string.booking_invalid_draft, Toast.LENGTH_SHORT).show();
            return;
        }

        BookingRequest req = new BookingRequest();
        req.doctorId = draft.doctorId;
        req.slotId = draft.slotId;
        req.appointmentDate = draft.slotDate;
        req.fullName = draft.fullName;
        req.email = draft.email;
        req.phoneNumber = draft.phone;
        req.dob = BookingFormatters.dobVnToIso(draft.dob);
        req.address = draft.address;
        req.gender = BookingFormatters.genderToEnum(draft.gender);
        req.notes = draft.notes;

        // Tối thiểu: phải có email để backend tìm/tạo Patient.
        if (TextUtils.isEmpty(req.email)) {
            Toast.makeText(this, R.string.booking_need_login, Toast.LENGTH_SHORT).show();
            return;
        }

        if (BookingPolicy.violatesMinLead(draft.slotDate, draft.startTime)) {
            Toast.makeText(this, getString(R.string.booking_min_lead_time_fmt, BookingPolicy.MIN_LEAD_MINUTES),
                    Toast.LENGTH_LONG).show();
            return;
        }

        SessionManager sm = new SessionManager(this);
        AuthSession s = sm.getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            Toast.makeText(this, R.string.booking_need_login, Toast.LENGTH_SHORT).show();
            return;
        }
        String bearer = "Bearer " + s.accessToken;

        setLoading(true);
        new BookingRepository().book(bearer, req, (data, error) -> {
            setLoading(false);
            if (error != null) {
                Toast.makeText(this, getString(R.string.booking_book_error_fmt, error),
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (data == null || data.appointmentId == null) {
                Toast.makeText(this, R.string.booking_book_error_generic, Toast.LENGTH_LONG).show();
                return;
            }
            openResult(data.appointmentId, data.appointmentDate, data.startTime, data.endTime);
        });
    }

    private void openResult(int appointmentId, @Nullable String apiDate,
                            @Nullable String apiStart, @Nullable String apiEnd) {
        Intent i = new Intent(this, BookingResultActivity.class);
        // Ưu tiên dữ liệu trả về từ backend (đã canonical hóa), fallback dùng draft.
        draft.slotDate = apiDate != null ? apiDate : draft.slotDate;
        draft.startTime = apiStart != null ? apiStart : draft.startTime;
        draft.endTime = apiEnd != null ? apiEnd : draft.endTime;
        draft.writeTo(i);
        i.putExtra(BookingResultActivity.EXTRA_APPOINTMENT_ID, appointmentId);
        startActivity(i);
        // Không finish() ngay để user có thể back lại review nếu muốn — nhưng UX tốt hơn
        // là close cả Step1+Step2 khi ấn Home ở Step3. Ở đây finish để chain sạch.
        finish();
    }

    private void setLoading(boolean loading) {
        if (loading) {
            if (loadingDialog == null) {
                loadingDialog = new ProgressDialog(this);
                loadingDialog.setMessage(getString(R.string.booking_submitting));
                loadingDialog.setCancelable(false);
            }
            confirmBtn.setEnabled(false);
            loadingDialog.show();
        } else {
            confirmBtn.setEnabled(true);
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    // ---------- Utils ----------

    private static String firstLetter(@Nullable String s) {
        if (s == null || s.isEmpty()) return "?";
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return "?";
        String[] parts = trimmed.split("\\s+");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? "?" : last.substring(0, 1).toUpperCase();
    }

    private void applyTopInsetToHeader(@NonNull View header) {
        final int basePad = header.getPaddingTop();
        final int startPad = header.getPaddingStart();
        final int endPad = header.getPaddingEnd();
        final int bottomPad = header.getPaddingBottom();
        final int extra = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPaddingRelative(startPad, basePad + topInset + extra, endPad, bottomPad);
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }
}
