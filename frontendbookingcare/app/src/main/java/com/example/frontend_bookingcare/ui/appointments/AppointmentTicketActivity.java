package com.example.frontend_bookingcare.ui.appointments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.DoctorDto;
import com.example.frontend_bookingcare.data.DoctorRepository;
import com.example.frontend_bookingcare.data.PatientProfileRepository;
import com.example.frontend_bookingcare.data.PatientAppointmentsRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.ProfileExtras;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.support.chat.CustomerCareChatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.frontend_bookingcare.ui.common.QrCodeUtil;

import com.example.frontend_bookingcare.ui.booking.BookingFormatters;
import com.example.frontend_bookingcare.ui.booking.BookingPolicy;

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

        String code = AppointmentCodes.appointmentCode(apptId, date);

        TextView tDoctor = findViewById(R.id.ticket_doctor_name);
        TextView tDoctor2 = findViewById(R.id.ticket_doctor_name2);
        TextView tStt = findViewById(R.id.ticket_stt);
        TextView tCode = findViewById(R.id.ticket_code);
        TextView tDate = findViewById(R.id.ticket_date);
        TextView tTime = findViewById(R.id.ticket_time);
        TextView statusBanner = findViewById(R.id.ticket_status_banner);
        ImageView qr = findViewById(R.id.ticket_qr);

        tDoctor.setText(doctorName != null ? doctorName : "—");
        tDoctor2.setText(doctorName != null ? doctorName : "—");
        tStt.setText(getString(R.string.dash_placeholder));
        tCode.setText(code);
        tDate.setText(formatDateDdMMyyyy(date));
        tTime.setText(BookingFormatters.timeRange(start, end));
        if (statusBanner != null) {
            statusBanner.setText(statusBannerFor(this, status));
        }

        // Patient info from cached profile extras
        SessionManager sm = new SessionManager(this);
        ProfileExtras ex = sm.getProfileExtras();
        TextView pCode = findViewById(R.id.ticket_patient_code);
        TextView pName = findViewById(R.id.ticket_patient_name);
        TextView pPhone = findViewById(R.id.ticket_patient_phone);
        TextView support = findViewById(R.id.ticket_support_phone);
        TextView doctorAddress = findViewById(R.id.ticket_doctor_address);
        if (doctorAddress != null) {
            doctorAddress.setText(getString(R.string.ticket_doctor_address_fmt, getString(R.string.dash_placeholder)));
            loadDoctorAddress(doctorId, doctorAddress);
        }

        int patientIdGuess = 0;
        try {
            if (sm.getSession() != null && !TextUtils.isEmpty(sm.getSession().userId)) {
                patientIdGuess = Integer.parseInt(sm.getSession().userId);
            }
        } catch (Exception ignored) {
        }
        pCode.setText(getString(R.string.patient_code_loading));
        pName.setText(sm.getSession() != null && !TextUtils.isEmpty(sm.getSession().fullName) ? sm.getSession().fullName : "—");
        pPhone.setText(!TextUtils.isEmpty(ex.phone) ? ex.phone : "—");

        // Update patientId from DB (for correct YMP code).
        if (sm.getSession() != null && !TextUtils.isEmpty(sm.getSession().accessToken)) {
            String bearer = "Bearer " + sm.getSession().accessToken;
            new PatientProfileRepository().fetchPatientId(bearer, (patientId, err) -> {
                if (patientId == null) return;
                runOnUiThread(() -> pCode.setText(AppointmentCodes.patientCode(patientId, date)));
            });
        }

        // QR code
        qr.setImageBitmap(QrCodeUtil.encode(code, dp(110), dp(110)));

        loadQueueNumber(apptId, tStt);

        // copy-on-tap
        tCode.setOnClickListener(v -> {
            copyToClipboard(getString(R.string.clipboard_label_ticket_code), code);
            Toast.makeText(this, R.string.ticket_code_copied, Toast.LENGTH_SHORT).show();
        });
        support.setOnClickListener(v -> {
            copyToClipboard(getString(R.string.clipboard_label_hotline), support.getText().toString());
            Toast.makeText(this, R.string.ticket_hotline_copied, Toast.LENGTH_SHORT).show();
        });

        int finalDoctorId = doctorId;
        findViewById(R.id.ticket_message).setOnClickListener(v -> {
            SessionManager chatSm = new SessionManager(this);
            if (!chatSm.isLoggedIn() || chatSm.getSession() == null
                    || TextUtils.isEmpty(chatSm.getSession().accessToken)) {
                Toast.makeText(this, R.string.chat_login_required, Toast.LENGTH_SHORT).show();
                return;
            }
            String displayName = doctorName != null && !doctorName.isEmpty()
                    ? doctorName : getString(R.string.dash_placeholder);
            String threadId = "doctor:" + Math.max(0, finalDoctorId);
            boolean locked = isChatLocked(status);
            startActivity(CustomerCareChatActivity.newIntent(
                    this,
                    threadId,
                    getString(R.string.chat_doctor_title_fmt, displayName),
                    getString(R.string.chat_doctor_subtitle_fmt, displayName),
                    locked
            ));
        });
        boolean canCancel = isCancellable(status);
        boolean canCheckIn = isCheckInAllowed(status, date);

        View checkInBtn = findViewById(R.id.ticket_check_in);
        TextView statusLabel = findViewById(R.id.ticket_status_label);
        statusLabel.setText(statusLabelFor(this, status));
        checkInBtn.setVisibility(canCheckIn ? android.view.View.VISIBLE : android.view.View.GONE);
        if (canCheckIn) {
            checkInBtn.setOnClickListener(v -> doCheckIn(apptId));
        }

        findViewById(R.id.ticket_cancel).setVisibility(canCancel ? android.view.View.VISIBLE : android.view.View.GONE);
        if (canCancel) {
            findViewById(R.id.ticket_cancel).setOnClickListener(v -> doCancel(apptId));
        }
    }

    private void loadDoctorAddress(int doctorId, TextView addressView) {
        if (doctorId <= 0) return;
        new DoctorRepository().fetchAllDoctors((list, err) -> runOnUiThread(() -> {
            if (list == null) return;
            for (DoctorDto d : list) {
                if (d != null && d.doctorId != null && d.doctorId == doctorId
                        && !TextUtils.isEmpty(d.clinicAddress)) {
                    addressView.setText(getString(R.string.ticket_doctor_address_fmt, d.clinicAddress));
                    return;
                }
            }
        }));
    }

    private void loadQueueNumber(int apptId, TextView sttView) {
        if (apptId <= 0) return;
        SessionManager sm = new SessionManager(this);
        if (sm.getSession() == null || TextUtils.isEmpty(sm.getSession().accessToken)) return;
        new PatientAppointmentsRepository().fetchDetail("Bearer " + sm.getSession().accessToken, apptId,
                (dto, err) -> runOnUiThread(() -> {
                    if (dto != null && dto.queueNumber != null && dto.queueNumber > 0) {
                        sttView.setText(String.valueOf(dto.queueNumber));
                    }
                }));
    }

    private static String statusLabelFor(android.content.Context ctx, @Nullable String status) {
        if (status == null || status.trim().isEmpty()) return "";
        switch (status.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "PENDING":
                return ctx.getString(R.string.ticket_status_label_pending);
            case "CONFIRMED":
                return ctx.getString(R.string.ticket_status_label_confirmed);
            case "CHECKED_IN":
                return ctx.getString(R.string.ticket_status_label_checked_in);
            case "COMPLETED":
                return ctx.getString(R.string.ticket_status_label_completed);
            case "CANCELLED":
                return ctx.getString(R.string.ticket_status_label_cancelled);
            case "NO_SHOW":
                return ctx.getString(R.string.ticket_status_label_no_show);
            default:
                return ctx.getString(R.string.ticket_status_label_unknown_fmt, status);
        }
    }

    private static boolean isCheckInAllowed(@Nullable String status, @Nullable String isoDate) {
        if (status == null || status.trim().isEmpty()) return false;
        if (!"CONFIRMED".equalsIgnoreCase(status.trim())) return false;
        if (isoDate == null || isoDate.length() < 10) return false;
        try {
            java.time.LocalDate apptDay = java.time.LocalDate.parse(isoDate.substring(0, 10));
            java.time.LocalDate today = java.time.LocalDate.now(BookingPolicy.CLINIC_ZONE);
            return today.equals(apptDay);
        } catch (Exception e) {
            return false;
        }
    }

    private static String statusBannerFor(android.content.Context ctx, @Nullable String status) {
        if (status == null || status.trim().isEmpty()) {
            return ctx.getString(R.string.appt_status_booked);
        }
        switch (status.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "PENDING":
                return ctx.getString(R.string.appt_status_pending);
            case "CONFIRMED":
                return ctx.getString(R.string.appt_status_confirmed);
            case "CHECKED_IN":
                return ctx.getString(R.string.appt_status_checked_in);
            case "COMPLETED":
                return ctx.getString(R.string.appt_status_completed);
            case "CANCELLED":
                return ctx.getString(R.string.appt_status_cancelled);
            case "NO_SHOW":
                return ctx.getString(R.string.appt_status_no_show);
            default:
                return status;
        }
    }

    private void doCheckIn(int apptId) {
        if (apptId <= 0) {
            Toast.makeText(this, R.string.ticket_appt_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        SessionManager sm = new SessionManager(this);
        if (!sm.isLoggedIn() || sm.getSession() == null || TextUtils.isEmpty(sm.getSession().accessToken)) {
            Toast.makeText(this, R.string.ticket_login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        findViewById(R.id.ticket_check_in).setEnabled(false);
        String bearer = "Bearer " + sm.getSession().accessToken;
        new PatientAppointmentsRepository().checkIn(bearer, apptId, (ok, err) -> runOnUiThread(() -> {
            findViewById(R.id.ticket_check_in).setEnabled(true);
            if (!ok) {
                Toast.makeText(this, err != null ? err : getString(R.string.ticket_check_in_failed), Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, R.string.appt_check_in_success, Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
        }));
    }

    private static boolean isCancellable(@Nullable String status) {
        if (status == null || status.trim().isEmpty()) return false;
        String s = status.trim().toLowerCase(java.util.Locale.ROOT);
        return "pending".equals(s) || "confirmed".equals(s);
    }

    private static boolean isChatLocked(@Nullable String status) {
        if (status == null || status.trim().isEmpty()) return false;
        String s = status.trim().toUpperCase(java.util.Locale.ROOT);
        return "CANCELLED".equals(s) || "NO_SHOW".equals(s) || "COMPLETED".equals(s);
    }

    private boolean onToolbarItem(MenuItem item, int apptId, String date) {
        if (item.getItemId() == R.id.action_share) {
            String code = AppointmentCodes.appointmentCode(apptId, date);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, getString(R.string.ticket_share_fmt, code));
            startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)));
            return true;
        }
        return false;
    }

    private void doCancel(int apptId) {
        if (apptId <= 0) {
            Toast.makeText(this, R.string.ticket_appt_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        SessionManager sm = new SessionManager(this);
        if (!sm.isLoggedIn() || sm.getSession() == null || TextUtils.isEmpty(sm.getSession().accessToken)) {
            Toast.makeText(this, R.string.ticket_cancel_login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.appt_cancel_confirm_title)
                .setMessage(R.string.appt_cancel_confirm_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.cancel_booking, (d, w) -> performCancel(apptId))
                .show();
    }

    private void performCancel(int apptId) {
        View cancelBtn = findViewById(R.id.ticket_cancel);
        cancelBtn.setEnabled(false);
        AuthSession session = new SessionManager(this).getSession();
        if (session == null || TextUtils.isEmpty(session.accessToken)) {
            cancelBtn.setEnabled(true);
            Toast.makeText(this, R.string.ticket_cancel_login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        String bearer = "Bearer " + session.accessToken;
        new PatientAppointmentsRepository().cancel(bearer, apptId, getString(R.string.appt_cancel_reason_patient), (env, err) -> runOnUiThread(() -> {
            cancelBtn.setEnabled(true);
            if (err != null) {
                Toast.makeText(this, getString(R.string.ticket_cancel_failed_fmt, err), Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, R.string.ticket_cancel_success, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }));
    }

    private static String formatDateDdMMyyyy(String yyyyMmDd) {
        try {
            LocalDate d = LocalDate.parse(yyyyMmDd);
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return yyyyMmDd != null ? yyyyMmDd : "—";
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

