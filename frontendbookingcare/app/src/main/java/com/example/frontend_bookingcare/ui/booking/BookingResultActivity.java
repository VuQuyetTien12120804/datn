package com.example.frontend_bookingcare.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.data.PatientProfileRepository;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.appointments.AppointmentCodes;
import com.example.frontend_bookingcare.ui.common.QrCodeUtil;
import com.example.frontend_bookingcare.ui.support.SupportBottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Bước 3: màn hình kết quả sau khi booking thành công.
 * Hiển thị mã lịch khám, số lịch, doctor + patient info, QR check-in, 2 nút: Về trang chủ / Hỗ trợ.
 */
public class BookingResultActivity extends AppCompatActivity {

    public static final String EXTRA_APPOINTMENT_ID = "br_appointment_id";
    public static final String EXTRA_QUEUE_NUMBER = "br_queue_number";

    private BookingDraft draft;
    private int appointmentId;
    private int queueNumber;
    private TextView patientCodeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_result);

        applyTopInsetToHeader(findViewById(R.id.booking_result_header));

        draft = BookingDraft.readFrom(getIntent());
        appointmentId = getIntent().getIntExtra(EXTRA_APPOINTMENT_ID, -1);
        queueNumber = getIntent().getIntExtra(EXTRA_QUEUE_NUMBER, -1);

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
        loadPatientCode();
        loadQueueNumberIfNeeded();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goHome();
            }
        });
    }

    private void loadQueueNumberIfNeeded() {
        if (queueNumber > 0 || appointmentId <= 0) {
            bindQueue();
            return;
        }
        SessionManager sm = new SessionManager(this);
        if (sm.getSession() == null || sm.getSession().accessToken == null || sm.getSession().accessToken.isEmpty()) {
            bindQueue();
            return;
        }
        new com.example.frontend_bookingcare.data.PatientAppointmentsRepository()
                .fetchDetail("Bearer " + sm.getSession().accessToken, appointmentId, (dto, err) ->
                        runOnUiThread(() -> {
                            if (dto != null && dto.queueNumber != null && dto.queueNumber > 0) {
                                queueNumber = dto.queueNumber;
                            }
                            bindQueue();
                        }));
    }

    private void loadPatientCode() {
        if (patientCodeView == null) return;
        SessionManager sm = new SessionManager(this);
        if (sm.getSession() == null || sm.getSession().accessToken == null || sm.getSession().accessToken.isEmpty()) {
            return;
        }
        String bearer = "Bearer " + sm.getSession().accessToken;
        new PatientProfileRepository().fetchPatientId(bearer, (patientId, err) -> runOnUiThread(() -> {
            if (patientId != null && patientId > 0) {
                patientCodeView.setText(AppointmentCodes.patientCode(patientId, draft.slotDate));
            }
        }));
    }

    private void bindSuccess() {
        TextView ts = findViewById(R.id.booking_result_timestamp);
        String formatted = ZonedDateTime.now(BookingPolicy.CLINIC_ZONE).format(
                DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy", Locale.getDefault()));
        ts.setText(formatted);
    }

    private void bindQueue() {
        TextView queue = findViewById(R.id.booking_result_queue);
        ImageView qrView = findViewById(R.id.booking_result_qr);
        int displayNo = queueNumber > 0 ? queueNumber : 0;
        queue.setText(displayNo > 0 ? String.valueOf(displayNo) : getString(R.string.appt_queue_pending));
        String code = AppointmentCodes.appointmentCode(appointmentId, draft.slotDate);
        if (qrView != null && appointmentId > 0) {
            int size = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 110, getResources().getDisplayMetrics());
            android.graphics.Bitmap qr = QrCodeUtil.encode(code, size, size);
            if (qr != null) {
                qrView.setImageBitmap(qr);
                qrView.setContentDescription(code);
            }
        }
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

        code.setText(AppointmentCodes.appointmentCode(appointmentId, draft.slotDate));
        date.setText(BookingFormatters.prettyDate(this, draft.slotDate));

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

        patientCodeView = pCode;
        pCode.setText(getString(R.string.patient_code_loading));
        name.setText(TextUtils.isEmpty(draft.fullName)
                ? getString(R.string.booking_not_available) : draft.fullName);
        dob.setText(TextUtils.isEmpty(draft.dob) ? getString(R.string.booking_not_updated) : draft.dob);
        gender.setText(BookingFormatters.displayGender(this, draft.gender));
        phone.setText(TextUtils.isEmpty(draft.phone)
                ? getString(R.string.booking_not_updated) : draft.phone);
    }

    // ---------- Navigation ----------

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finishAffinity();
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
