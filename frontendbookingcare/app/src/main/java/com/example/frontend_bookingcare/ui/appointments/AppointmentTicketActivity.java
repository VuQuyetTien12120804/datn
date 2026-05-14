package com.example.frontend_bookingcare.ui.appointments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.data.PatientProfileRepository;
import com.example.frontend_bookingcare.data.PatientAppointmentsRepository;
import com.example.frontend_bookingcare.session.ProfileExtras;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.support.chat.CustomerCareChatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AppointmentTicketActivity extends AppCompatActivity {

    private static final String EXTRA_APPT_ID = "appt_id";
    private static final String EXTRA_DOCTOR_ID = "doctor_id";
    private static final String EXTRA_DOCTOR_NAME = "doctor_name";
    private static final String EXTRA_DATE = "date"; // yyyy-MM-dd
    private static final String EXTRA_START = "start"; // HH:mm
    private static final String EXTRA_END = "end"; // HH:mm
    private static final String EXTRA_STATUS = "status";

    public static Intent newIntent(
            Context ctx,
            int appointmentId,
            int doctorId,
            String doctorName,
            String date,
            String start,
            String end,
            @Nullable String status
    ) {
        Intent i = new Intent(ctx, AppointmentTicketActivity.class);
        i.putExtra(EXTRA_APPT_ID, appointmentId);
        i.putExtra(EXTRA_DOCTOR_ID, doctorId);
        i.putExtra(EXTRA_DOCTOR_NAME, doctorName);
        i.putExtra(EXTRA_DATE, date);
        i.putExtra(EXTRA_START, start);
        i.putExtra(EXTRA_END, end);
        i.putExtra(EXTRA_STATUS, status);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_appointment_ticket);

        int apptId = getIntent().getIntExtra(EXTRA_APPT_ID, 0);
        int doctorId = getIntent().getIntExtra(EXTRA_DOCTOR_ID, 0);
        String doctorName = getIntent().getStringExtra(EXTRA_DOCTOR_NAME);
        String date = getIntent().getStringExtra(EXTRA_DATE);
        String start = getIntent().getStringExtra(EXTRA_START);
        String end = getIntent().getStringExtra(EXTRA_END);
        String status = getIntent().getStringExtra(EXTRA_STATUS);

        MaterialToolbar tb = findViewById(R.id.ticket_toolbar);
        applyTopInsetToToolbar(tb);
        tb.setNavigationOnClickListener(v -> finish());
        tb.inflateMenu(R.menu.menu_ticket);
        tb.setOnMenuItemClickListener(item -> onToolbarItem(item, apptId, date));

        String ddMMyyyy = formatDateDdMMyyyy(date);
        String timeRange = (start != null ? start : "") + (end != null && !TextUtils.isEmpty(end) ? ("-" + end) : "");
        String code = buildAppointmentCode(apptId, date);

        TextView tDoctor = findViewById(R.id.ticket_doctor_name);
        TextView tDoctor2 = findViewById(R.id.ticket_doctor_name2);
        TextView tStt = findViewById(R.id.ticket_stt);
        TextView tCode = findViewById(R.id.ticket_code);
        TextView tDate = findViewById(R.id.ticket_date);
        TextView tTime = findViewById(R.id.ticket_time);
        ImageView qr = findViewById(R.id.ticket_qr);

        tDoctor.setText(doctorName != null ? doctorName : "—");
        tDoctor2.setText(doctorName != null ? doctorName : "—");
        tStt.setText(String.valueOf(apptId));
        tCode.setText(code);
        tDate.setText(ddMMyyyy);
        tTime.setText(timeRange);

        // Patient info from cached profile extras
        SessionManager sm = new SessionManager(this);
        ProfileExtras ex = sm.getProfileExtras();
        TextView pCode = findViewById(R.id.ticket_patient_code);
        TextView pName = findViewById(R.id.ticket_patient_name);
        TextView pPhone = findViewById(R.id.ticket_patient_phone);
        TextView support = findViewById(R.id.ticket_support_phone);

        int patientIdGuess = 0;
        try {
            if (sm.getSession() != null && !TextUtils.isEmpty(sm.getSession().userId)) {
                patientIdGuess = Integer.parseInt(sm.getSession().userId);
            }
        } catch (Exception ignored) {
        }
        pCode.setText(buildPatientCode(patientIdGuess, date));
        pName.setText(sm.getSession() != null && !TextUtils.isEmpty(sm.getSession().fullName) ? sm.getSession().fullName : "—");
        pPhone.setText(!TextUtils.isEmpty(ex.phone) ? ex.phone : "—");

        // Update patientId from DB (for correct YMP code).
        if (sm.getSession() != null && !TextUtils.isEmpty(sm.getSession().accessToken)) {
            String bearer = "Bearer " + sm.getSession().accessToken;
            new PatientProfileRepository().fetchPatientId(bearer, (patientId, err) -> {
                if (patientId == null) return;
                runOnUiThread(() -> pCode.setText(buildPatientCode(patientId, date)));
            });
        }

        // QR code
        qr.setImageBitmap(makeQrBitmap(code, dp(110), dp(110)));

        // copy-on-tap
        tCode.setOnClickListener(v -> {
            copyToClipboard("Mã phiếu khám", code);
            Toast.makeText(this, "Đã copy mã phiếu khám", Toast.LENGTH_SHORT).show();
        });
        support.setOnClickListener(v -> {
            copyToClipboard("Hotline", support.getText().toString());
            Toast.makeText(this, "Đã copy hotline", Toast.LENGTH_SHORT).show();
        });

        int finalDoctorId = doctorId;
        findViewById(R.id.ticket_message).setOnClickListener(v -> {
            String dName = doctorName != null ? doctorName : "Bác sĩ";
            String threadId = "doctor:" + Math.max(0, finalDoctorId);
            boolean locked = isChatLocked(status);
            startActivity(CustomerCareChatActivity.newIntent(
                    this,
                    threadId,
                    "Chăm Sóc Khách Hàng",
                    "Bác sĩ: " + dName,
                    locked
            ));
        });
        boolean canCancel = isCancellable(status);
        findViewById(R.id.ticket_cancel).setVisibility(canCancel ? android.view.View.VISIBLE : android.view.View.GONE);
        if (canCancel) {
            findViewById(R.id.ticket_cancel).setOnClickListener(v -> doCancel(apptId));
        }
    }

    private static boolean isCancellable(@Nullable String status) {
        if (status == null || status.trim().isEmpty()) return false;
        String s = status.trim().toLowerCase(java.util.Locale.ROOT);
        return "pending".equals(s) || "confirmed".equals(s);
    }

    private static boolean isChatLocked(@Nullable String status) {
        if (status == null || status.trim().isEmpty()) return false;
        String s = status.trim().toLowerCase(java.util.Locale.ROOT);
        // Locked chat = user đang có lịch hẹn còn hiệu lực (không phải thread support chung).
        return "pending".equals(s)
                || "confirmed".equals(s)
                || "checked_in".equals(s);
    }

    private boolean onToolbarItem(MenuItem item, int apptId, String date) {
        if (item.getItemId() == R.id.action_share) {
            String code = buildAppointmentCode(apptId, date);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, "Phiếu khám: " + code);
            startActivity(Intent.createChooser(send, "Chia sẻ"));
            return true;
        }
        return false;
    }

    private void doCancel(int apptId) {
        if (apptId <= 0) {
            Toast.makeText(this, "Không tìm thấy mã lịch hẹn", Toast.LENGTH_SHORT).show();
            return;
        }
        SessionManager sm = new SessionManager(this);
        if (!sm.isLoggedIn() || sm.getSession() == null || TextUtils.isEmpty(sm.getSession().accessToken)) {
            Toast.makeText(this, "Bạn cần đăng nhập để huỷ lịch", Toast.LENGTH_SHORT).show();
            return;
        }
        findViewById(R.id.ticket_cancel).setEnabled(false);
        String bearer = "Bearer " + sm.getSession().accessToken;
        new PatientAppointmentsRepository().cancel(bearer, apptId, "Bệnh nhân huỷ", (env, err) -> runOnUiThread(() -> {
            findViewById(R.id.ticket_cancel).setEnabled(true);
            if (err != null) {
                Toast.makeText(this, "Huỷ lịch thất bại: " + err, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Đã huỷ lịch", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }));
    }

    private static String buildAppointmentCode(int apptId, String yyyyMmDd) {
        String yyMMdd = "000000";
        try {
            LocalDate d = LocalDate.parse(yyyyMmDd);
            yyMMdd = d.format(DateTimeFormatter.ofPattern("yyMMdd"));
        } catch (Exception ignored) {
        }
        return "YMA" + yyMMdd + String.format("%04d", Math.max(apptId, 0));
    }

    private static String buildPatientCode(int patientId, String yyyyMmDd) {
        String yyMMdd = "000000";
        try {
            LocalDate d = LocalDate.parse(yyyyMmDd);
            yyMMdd = d.format(DateTimeFormatter.ofPattern("yyMMdd"));
        } catch (Exception ignored) {
        }
        if (patientId <= 0) {
            return "YMP" + yyMMdd + "----";
        }
        return "YMP" + yyMMdd + String.format("%04d", patientId);
    }

    private static String formatDateDdMMyyyy(String yyyyMmDd) {
        try {
            LocalDate d = LocalDate.parse(yyyyMmDd);
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return yyyyMmDd != null ? yyyyMmDd : "—";
        }
    }

    private Bitmap makeQrBitmap(String content, int w, int h) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix m = writer.encode(content, BarcodeFormat.QR_CODE, w, h);
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    b.setPixel(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return b;
        } catch (WriterException e) {
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            b.eraseColor(Color.LTGRAY);
            return b;
        }
    }

    private void copyToClipboard(String label, String value) {
        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cb != null) {
            cb.setPrimaryClip(ClipData.newPlainText(label, value));
        }
    }

    private void applyTopInsetToToolbar(MaterialToolbar toolbar) {
        final int basePadTop = toolbar.getPaddingTop();
        final int basePadBottom = toolbar.getPaddingBottom();
        final int basePadStart = toolbar.getPaddingStart();
        final int basePadEnd = toolbar.getPaddingEnd();
        final int extra = dp(6); // xuống dưới camera 1 chút

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPaddingRelative(basePadStart, basePadTop + topInset + extra, basePadEnd, basePadBottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(toolbar);
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}

