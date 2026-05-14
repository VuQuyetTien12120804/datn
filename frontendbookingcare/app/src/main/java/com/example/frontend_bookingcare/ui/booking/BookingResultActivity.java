package com.example.frontend_bookingcare.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.ui.support.SupportBottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Bước 3: màn hình kết quả sau khi booking thành công.
 * Hiển thị mã lịch khám, STT (demo), doctor + patient info, 2 nút: Về trang chủ / Hỗ trợ.
 */
public class BookingResultActivity extends AppCompatActivity {

    public static final String EXTRA_APPOINTMENT_ID = "br_appointment_id";

    private BookingDraft draft;
    private int appointmentId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_result);

        applyTopInsetToHeader(findViewById(R.id.booking_result_header));

        draft = BookingDraft.readFrom(getIntent());
        appointmentId = getIntent().getIntExtra(EXTRA_APPOINTMENT_ID, -1);

        ImageButton close = findViewById(R.id.booking_result_close);
        close.setOnClickListener(v -> goHome());

        MaterialButton homeBtn = findViewById(R.id.booking_result_home);
        MaterialButton supportBtn = findViewById(R.id.booking_result_support);
        homeBtn.setOnClickListener(v -> goHome());
        supportBtn.setOnClickListener(v ->
                SupportBottomSheetDialogFragment.newInstance().show(getSupportFragmentManager(), "support_sheet"));

        bindSuccess();
        bindQueue();
        bindDoctor();
        bindAppointment();
        bindPatient();
    }

    private void bindSuccess() {
        TextView ts = findViewById(R.id.booking_result_timestamp);
        String formatted = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy", Locale.getDefault()));
        ts.setText(formatted);
    }

    private void bindQueue() {
        TextView queue = findViewById(R.id.booking_result_queue);
        TextView qrText = findViewById(R.id.booking_result_qr_text);
        // STT đơn giản = appointmentId % 100 (backend chưa có trường queue number riêng).
        int stt = appointmentId > 0 ? Math.max(1, appointmentId % 100) : 1;
        queue.setText(String.valueOf(stt));
        // "QR" placeholder: hiển thị text mã lịch khám ở giữa ô vuông. Sau này
        // có thể thay bằng thư viện ZXing để render QR thật.
        qrText.setText(buildAppointmentCode(appointmentId));
    }

    private void bindDoctor() {
        FrameLayout avatar = findViewById(R.id.booking_result_doctor_avatar);
        TextView letter = findViewById(R.id.booking_result_doctor_letter);
        TextView name = findViewById(R.id.booking_result_doctor_name);

        avatar.setBackgroundResource(draft.doctorAvatarBg != 0
                ? draft.doctorAvatarBg : R.drawable.bg_tile_blue);
        letter.setText(firstLetter(draft.doctorName));

        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(draft.doctorTitle)) sb.append(draft.doctorTitle).append(' ');
        if (!TextUtils.isEmpty(draft.doctorName)) sb.append(draft.doctorName);
        name.setText(sb.toString().trim());
    }

    private void bindAppointment() {
        TextView code = findViewById(R.id.booking_result_code);
        TextView date = findViewById(R.id.booking_result_date);
        TextView time = findViewById(R.id.booking_result_time);

        code.setText(buildAppointmentCode(appointmentId));
        date.setText(BookingFormatters.prettyDate(draft.slotDate));

        String range = BookingFormatters.timeRange(draft.startTime, draft.endTime);
        String session = getString("morning".equals(draft.session)
                ? R.string.booking_session_morning : R.string.booking_session_afternoon);
        time.setText(getString(R.string.booking_result_time_fmt, range, session));
    }

    private void bindPatient() {
        TextView pCode = findViewById(R.id.booking_result_patient_code);
        TextView name = findViewById(R.id.booking_result_patient_name);
        TextView dob = findViewById(R.id.booking_result_patient_dob);
        TextView gender = findViewById(R.id.booking_result_patient_gender);
        TextView phone = findViewById(R.id.booking_result_patient_phone);

        pCode.setText(buildPatientCode(draft.email));
        name.setText(TextUtils.isEmpty(draft.fullName)
                ? getString(R.string.booking_not_available) : draft.fullName);
        dob.setText(TextUtils.isEmpty(draft.dob) ? getString(R.string.booking_not_updated) : draft.dob);
        gender.setText(BookingFormatters.displayGender(draft.gender));
        phone.setText(TextUtils.isEmpty(draft.phone)
                ? getString(R.string.booking_not_updated) : draft.phone);
    }

    // ---------- Navigation ----------

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    // ---------- Utils ----------

    /** Mã lịch khám dạng YMA + ngày tháng + id — để bệnh nhân tra cứu. */
    private static String buildAppointmentCode(int appointmentId) {
        String day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHH"));
        return "YMA" + day + String.format(Locale.US, "%04d", Math.max(0, appointmentId % 10000));
    }

    /** Mã bệnh nhân — hash nhanh từ email để ổn định giữa các lần đặt. */
    private static String buildPatientCode(@Nullable String email) {
        if (email == null || email.isEmpty()) return "YMP00000000";
        long hash = 0;
        for (int i = 0; i < email.length(); i++) hash = hash * 31 + email.charAt(i);
        hash = Math.abs(hash);
        return "YMP" + String.format(Locale.US, "%09d", hash % 1_000_000_000L);
    }

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
