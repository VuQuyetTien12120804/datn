package com.example.frontend_bookingcare.ui.doctor_panel;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.DoctorAppointmentDto;
import com.example.frontend_bookingcare.data.DoctorPanelRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.booking.BookingFormatters;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tab khám bệnh: một ca làm việc tập trung — bệnh nhân tiếp theo / đang khám, ghi chú, trạng thái.
 * Dữ liệu lấy từ {@link DoctorPanelRepository#fetchToday}; nút bắt đầu khám chỉ đổi UI cục bộ cho đến khi có API.
 */
public class DoctorExaminingFragment extends Fragment {

    private final DoctorPanelRepository repository = new DoctorPanelRepository();

    private TextView statusChip;
    private EditText notesField;
    private MaterialButton startBtn;
    private LinearLayout contentLayout;
    private LinearLayout emptyLayout;
    private MaterialCardView overdueBanner;
    private TextView examAvatar;
    private TextView patientName;
    private TextView ageGender;
    private TextView dateView;
    private TextView timeView;
    private TextView bookingReason;
    private TextView phoneView;
    private TextView addressView;
    private TextView apptMeta;

    @Nullable private DoctorAppointmentDto bound;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_examining, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DoctorUiHelper.applyTopInsetPadding(view, 12);

        statusChip = view.findViewById(R.id.doctor_exam_status);
        notesField = view.findViewById(R.id.doctor_exam_notes);
        startBtn = view.findViewById(R.id.doctor_exam_start_btn);
        contentLayout = view.findViewById(R.id.doctor_exam_content);
        emptyLayout = view.findViewById(R.id.doctor_exam_empty);
        overdueBanner = view.findViewById(R.id.doctor_exam_overdue_banner);
        examAvatar = view.findViewById(R.id.doctor_exam_avatar);
        patientName = view.findViewById(R.id.doctor_exam_patient);
        ageGender = view.findViewById(R.id.doctor_exam_age_gender);
        dateView = view.findViewById(R.id.doctor_exam_date);
        timeView = view.findViewById(R.id.doctor_exam_time);
        bookingReason = view.findViewById(R.id.doctor_exam_booking_reason);
        phoneView = view.findViewById(R.id.doctor_exam_phone);
        addressView = view.findViewById(R.id.doctor_exam_address);
        apptMeta = view.findViewById(R.id.doctor_exam_appt_meta);

        startBtn.setOnClickListener(v -> onStartExamClicked());

        loadTodayAppointment();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTodayAppointment();
    }

    private void loadTodayAppointment() {
        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            showEmptyState();
            return;
        }
        repository.fetchToday("Bearer " + s.accessToken, (list, err) -> {
            if (!isAdded()) return;
            if (err != null) {
                Toast.makeText(requireContext(), getString(R.string.doctor_panel_load_failed_fmt, err), Toast.LENGTH_LONG).show();
                showEmptyState();
                return;
            }
            DoctorAppointmentDto pick = pickTarget(list != null ? list : Collections.emptyList());
            if (pick == null) {
                showEmptyState();
            } else {
                bindAppointment(pick);
            }
        });
    }

    /**
     * Ưu tiên IN_PROGRESS; sau đó lịch chưa hoàn thành/hủy, sắp xếp theo thời điểm bắt đầu slot.
     */
    @Nullable
    private static DoctorAppointmentDto pickTarget(@NonNull List<DoctorAppointmentDto> list) {
        DoctorAppointmentDto inProgress = null;
        List<DoctorAppointmentDto> candidates = new ArrayList<>();
        for (DoctorAppointmentDto d : list) {
            if (d == null) continue;
            String st = statusUpper(d.status);
            if ("IN_PROGRESS".equals(st)) {
                inProgress = d;
                break;
            }
            if ("COMPLETED".equals(st) || "CANCELLED".equals(st)) {
                continue;
            }
            candidates.add(d);
        }
        if (inProgress != null) {
            return inProgress;
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator.comparingLong(DoctorExaminingFragment::slotStartOrMax));
        return candidates.get(0);
    }

    private static long slotStartOrMax(@Nullable DoctorAppointmentDto d) {
        if (d == null) return Long.MAX_VALUE;
        Long ms = DoctorPanelFormatters.appointmentSlotStartMillis(d.appointmentDate, d.expectedTime);
        return ms != null ? ms : Long.MAX_VALUE;
    }

    @Nullable
    private static String statusUpper(@Nullable String status) {
        if (status == null) return "";
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private void showEmptyState() {
        bound = null;
        contentLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);
        overdueBanner.setVisibility(View.GONE);
        resetStatusIdle();
        notesField.setEnabled(false);
        notesField.setText("");
        notesField.setHint(R.string.doctor_exam_notes_hint);
    }

    private void bindAppointment(@NonNull DoctorAppointmentDto d) {
        bound = d;
        contentLayout.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);

        String name = TextUtils.isEmpty(d.patientName) ? "—" : d.patientName;
        DoctorPanelUi.stylePatientAvatar(examAvatar, name, requireContext());
        patientName.setText(name);

        int age = DoctorPanelFormatters.resolvePatientAge(d.dob, d.patientAge);
        String gender = DoctorPanelFormatters.displayGender(d.gender);
        if (age > 0) {
            ageGender.setText(getString(R.string.doctor_age_gender_fmt, age, gender));
        } else {
            ageGender.setText(gender);
        }

        dateView.setText(BookingFormatters.prettyDate(d.appointmentDate));
        timeView.setText(DoctorPanelFormatters.formatTimeAmPm(d.expectedTime));

        if (d.appointmentId != null && d.appointmentId > 0) {
            apptMeta.setVisibility(View.VISIBLE);
            apptMeta.setText(getString(R.string.doctor_exam_appt_id_fmt, d.appointmentId));
        } else {
            apptMeta.setVisibility(View.GONE);
        }

        String note = d.notes != null ? d.notes.trim() : "";
        if (TextUtils.isEmpty(note) || "—".equals(note)) {
            bookingReason.setVisibility(View.GONE);
        } else {
            bookingReason.setVisibility(View.VISIBLE);
            bookingReason.setText(getString(R.string.doctor_exam_booking_reason_fmt, note));
        }

        String phone = !TextUtils.isEmpty(d.phoneNumber) ? d.phoneNumber : d.phone;
        if (TextUtils.isEmpty(phone)) {
            phoneView.setVisibility(View.GONE);
        } else {
            phoneView.setVisibility(View.VISIBLE);
            phoneView.setText(phone);
        }
        addressView.setVisibility(View.GONE);

        boolean inProg = "IN_PROGRESS".equals(statusUpper(d.status));
        boolean overdue = isPastSlot(d) && !inProg;
        overdueBanner.setVisibility(overdue ? View.VISIBLE : View.GONE);

        if (inProg) {
            applyInProgressUi(false);
        } else {
            resetStatusIdle();
            notesField.setEnabled(false);
            notesField.setText("");
            notesField.setHint(R.string.doctor_exam_notes_hint);
            startBtn.setEnabled(true);
            startBtn.setAlpha(1f);
            startBtn.setText(R.string.doctor_exam_start);
        }
    }

    private static boolean isPastSlot(@NonNull DoctorAppointmentDto d) {
        Long ms = DoctorPanelFormatters.appointmentSlotStartMillis(d.appointmentDate, d.expectedTime);
        if (ms != null) {
            return ms < System.currentTimeMillis();
        }
        return DoctorPanelFormatters.isStrictlyPastAppointmentDay(d.appointmentDate);
    }

    private void resetStatusIdle() {
        statusChip.setText(R.string.doctor_exam_status_idle);
        statusChip.setBackgroundResource(R.drawable.bg_doctor_status_wait);
        statusChip.setTextColor(getResources().getColor(R.color.doctor_status_wait_fg, null));
    }

    private void applyInProgressUi(boolean showToast) {
        statusChip.setText(R.string.doctor_status_progress);
        statusChip.setBackgroundResource(R.drawable.bg_doctor_status_progress);
        statusChip.setTextColor(getResources().getColor(R.color.doctor_status_progress_fg, null));
        notesField.setEnabled(true);
        notesField.setHint("");
        notesField.requestFocus();
        startBtn.setEnabled(false);
        startBtn.setAlpha(0.55f);
        startBtn.setText(R.string.doctor_exam_in_progress_btn);
        if (showToast) {
            Toast.makeText(requireContext(), R.string.doctor_exam_start, Toast.LENGTH_SHORT).show();
        }
    }

    private void onStartExamClicked() {
        if (bound == null) return;
        if ("IN_PROGRESS".equals(statusUpper(bound.status))) {
            return;
        }
        applyInProgressUi(true);
    }
}
