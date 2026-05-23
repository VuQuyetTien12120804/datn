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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.DoctorAppointmentDetailDto;
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
 * Tab khám bệnh — phiên khám thật qua API (bắt đầu / ghi chú / hoàn thành / no-show).
 */
public class DoctorExaminingFragment extends Fragment {

    private static final String ARG_APPOINTMENT_ID = "appointmentId";

    private final DoctorPanelRepository repository = new DoctorPanelRepository();

    private TextView statusChip;
    private EditText notesField;
    private MaterialButton startBtn;
    private MaterialButton saveNotesBtn;
    private MaterialButton completeBtn;
    private MaterialButton noShowBtn;
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

    @Nullable private DoctorAppointmentDetailDto bound;
    private int focusAppointmentId;

    public static DoctorExaminingFragment newInstance(int appointmentId) {
        DoctorExaminingFragment f = new DoctorExaminingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_APPOINTMENT_ID, appointmentId);
        f.setArguments(args);
        return f;
    }

    public void openAppointment(int appointmentId) {
        focusAppointmentId = appointmentId;
        if (isAdded()) {
            loadAppointment(appointmentId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_examining, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DoctorUiHelper.applyTopInsetPadding(view.findViewById(R.id.doctor_exam_root), 16);

        statusChip = view.findViewById(R.id.doctor_exam_status);
        notesField = view.findViewById(R.id.doctor_exam_notes);
        startBtn = view.findViewById(R.id.doctor_exam_start_btn);
        saveNotesBtn = view.findViewById(R.id.doctor_exam_save_notes_btn);
        completeBtn = view.findViewById(R.id.doctor_exam_complete_btn);
        noShowBtn = view.findViewById(R.id.doctor_exam_no_show_btn);
        contentLayout = view.findViewById(R.id.doctor_exam_content);
        emptyLayout = view.findViewById(R.id.doctor_exam_empty);
        if (emptyLayout != null) {
            DoctorEmptyUi.bind(emptyLayout,
                    R.drawable.ic_nav_doctor_clinic,
                    R.string.doctor_exam_empty_title,
                    R.string.doctor_exam_empty_hint);
        }
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

        startBtn.setOnClickListener(v -> onStartExam());
        saveNotesBtn.setOnClickListener(v -> onSaveNotes(false));
        completeBtn.setOnClickListener(v -> confirmComplete());
        noShowBtn.setOnClickListener(v -> confirmNoShow());

        Bundle args = getArguments();
        if (args != null) {
            focusAppointmentId = args.getInt(ARG_APPOINTMENT_ID, 0);
        }
        resolveAndLoad();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (focusAppointmentId > 0) {
            loadAppointment(focusAppointmentId);
        } else {
            resolveAndLoad();
        }
    }

    private void resolveAndLoad() {
        if (focusAppointmentId > 0) {
            loadAppointment(focusAppointmentId);
            return;
        }
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
            if (pick == null || pick.appointmentId == null || pick.appointmentId <= 0) {
                showEmptyState();
            } else {
                loadAppointment(pick.appointmentId);
            }
        });
    }

    private void loadAppointment(int appointmentId) {
        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            showEmptyState();
            return;
        }
        setButtonsEnabled(false);
        repository.fetchAppointmentDetail("Bearer " + s.accessToken, appointmentId, (dto, err) -> {
            if (!isAdded()) return;
            setButtonsEnabled(true);
            if (err != null || dto == null) {
                Toast.makeText(requireContext(), getString(R.string.doctor_panel_load_failed_fmt, err != null ? err : ""), Toast.LENGTH_LONG).show();
                showEmptyState();
                return;
            }
            bindDetail(dto);
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        startBtn.setEnabled(enabled);
        saveNotesBtn.setEnabled(enabled);
        completeBtn.setEnabled(enabled);
        noShowBtn.setEnabled(enabled);
    }

    @Nullable
    private static DoctorAppointmentDto pickTarget(@NonNull List<DoctorAppointmentDto> list) {
        DoctorAppointmentDto checkedIn = null;
        List<DoctorAppointmentDto> candidates = new ArrayList<>();
        for (DoctorAppointmentDto d : list) {
            if (d == null) continue;
            String st = statusUpper(d.status);
            if ("CHECKED_IN".equals(st)) {
                checkedIn = d;
            }
            if ("COMPLETED".equals(st) || "CANCELLED".equals(st) || "NO_SHOW".equals(st) || "PENDING".equals(st)) {
                continue;
            }
            candidates.add(d);
        }
        if (checkedIn != null) {
            return checkedIn;
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
    }

    private void bindDetail(@NonNull DoctorAppointmentDetailDto d) {
        bound = d;
        contentLayout.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);

        String name = TextUtils.isEmpty(d.patientName) ? "—" : d.patientName;
        DoctorPanelUi.stylePatientAvatar(examAvatar, name, requireContext());
        patientName.setText(name);

        int age = DoctorPanelFormatters.ageFromDateString(d.patientDob);
        String gender = DoctorPanelFormatters.displayGender(d.patientGender);
        if (age > 0) {
            ageGender.setText(getString(R.string.doctor_age_gender_fmt, age, gender));
        } else {
            ageGender.setText(gender);
        }

        String apptDate = !TextUtils.isEmpty(d.appointmentDate) ? d.appointmentDate : d.startsAt;
        dateView.setText(BookingFormatters.prettyDate(apptDate));
        timeView.setText(DoctorPanelFormatters.formatTimeHm(d.expectedTime));

        if (d.appointmentId != null && d.appointmentId > 0) {
            apptMeta.setVisibility(View.VISIBLE);
            apptMeta.setText(getString(R.string.doctor_exam_appt_id_fmt, d.appointmentId));
        } else {
            apptMeta.setVisibility(View.GONE);
        }

        String reason = d.reason != null ? d.reason.trim() : "";
        if (TextUtils.isEmpty(reason) || "—".equals(reason)) {
            bookingReason.setVisibility(View.GONE);
        } else {
            bookingReason.setVisibility(View.VISIBLE);
            bookingReason.setText(getString(R.string.doctor_exam_booking_reason_fmt, reason));
        }

        if (TextUtils.isEmpty(d.patientPhone)) {
            phoneView.setVisibility(View.GONE);
        } else {
            phoneView.setVisibility(View.VISIBLE);
            phoneView.setText(d.patientPhone);
        }

        if (TextUtils.isEmpty(d.patientAddress)) {
            addressView.setVisibility(View.GONE);
        } else {
            addressView.setVisibility(View.VISIBLE);
            addressView.setText(d.patientAddress);
        }

        String st = statusUpper(d.status);
        boolean examining = "CHECKED_IN".equals(st);
        boolean completed = "COMPLETED".equals(st);
        boolean canStart = "CONFIRMED".equals(st) || "CHECKED_IN".equals(st);
        boolean overdue = isPastSlot(d) && !examining && !completed;

        overdueBanner.setVisibility(overdue ? View.VISIBLE : View.GONE);

        if (examining) {
            applyExaminingUi();
        } else if (completed) {
            applyCompletedUi();
        } else {
            applyIdleUi(canStart);
        }

        notesField.setText(d.note != null ? d.note : "");
    }

    private static boolean isPastSlot(@NonNull DoctorAppointmentDetailDto d) {
        Long ms = DoctorPanelFormatters.appointmentSlotStartMillis(d.appointmentDate, d.expectedTime);
        if (ms != null) {
            return ms < System.currentTimeMillis();
        }
        return DoctorPanelFormatters.isStrictlyPastAppointmentDay(d.appointmentDate);
    }

    private void applyIdleUi(boolean canStart) {
        statusChip.setText(R.string.doctor_exam_status_idle);
        statusChip.setBackgroundResource(R.drawable.bg_doctor_hero_chip);
        statusChip.setTextColor(getResources().getColor(R.color.white, null));
        notesField.setEnabled(false);
        notesField.setHint(R.string.doctor_exam_notes_hint);
        startBtn.setVisibility(canStart ? View.VISIBLE : View.GONE);
        startBtn.setEnabled(true);
        startBtn.setText(R.string.doctor_exam_start);
        saveNotesBtn.setVisibility(View.GONE);
        completeBtn.setVisibility(View.GONE);
        noShowBtn.setVisibility(canMarkNoShow() ? View.VISIBLE : View.GONE);
    }

    private void applyExaminingUi() {
        statusChip.setText(R.string.doctor_status_progress);
        statusChip.setBackgroundResource(R.drawable.bg_doctor_status_progress);
        statusChip.setTextColor(getResources().getColor(R.color.white, null));
        notesField.setEnabled(true);
        notesField.setHint("");
        startBtn.setVisibility(View.VISIBLE);
        startBtn.setEnabled(false);
        startBtn.setText(R.string.doctor_exam_in_progress_btn);
        saveNotesBtn.setVisibility(View.VISIBLE);
        completeBtn.setVisibility(View.VISIBLE);
        noShowBtn.setVisibility(View.GONE);
    }

    private void applyCompletedUi() {
        statusChip.setText(R.string.doctor_status_done);
        statusChip.setBackgroundResource(R.drawable.bg_doctor_status_done);
        statusChip.setTextColor(getResources().getColor(R.color.doctor_status_done_fg, null));
        notesField.setEnabled(false);
        startBtn.setVisibility(View.GONE);
        saveNotesBtn.setVisibility(View.GONE);
        completeBtn.setVisibility(View.GONE);
        noShowBtn.setVisibility(View.GONE);
    }

    private boolean canMarkNoShow() {
        if (bound == null) return false;
        String st = statusUpper(bound.status);
        return "CONFIRMED".equals(st) && isPastSlot(bound);
    }

    @Nullable
    private String bearer() {
        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            Toast.makeText(requireContext(), R.string.doctor_panel_session_required, Toast.LENGTH_SHORT).show();
            return null;
        }
        return "Bearer " + s.accessToken;
    }

    private int appointmentIdOrZero() {
        return bound != null && bound.appointmentId != null ? bound.appointmentId : 0;
    }

    private void onStartExam() {
        int id = appointmentIdOrZero();
        String auth = bearer();
        if (id <= 0 || auth == null) return;
        setButtonsEnabled(false);
        repository.startExam(auth, id, (dto, err) -> {
            if (!isAdded()) return;
            setButtonsEnabled(true);
            if (err != null || dto == null) {
                Toast.makeText(requireContext(), getString(R.string.doctor_panel_action_failed_fmt, err != null ? err : ""), Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(requireContext(), R.string.doctor_exam_started_toast, Toast.LENGTH_SHORT).show();
            bindDetail(dto);
            notesField.requestFocus();
        });
    }

    private void onSaveNotes(boolean silent) {
        int id = appointmentIdOrZero();
        String auth = bearer();
        if (id <= 0 || auth == null) return;
        String note = notesField.getText() != null ? notesField.getText().toString().trim() : "";
        setButtonsEnabled(false);
        repository.saveClinicalNote(auth, id, note, (dto, err) -> {
            if (!isAdded()) return;
            setButtonsEnabled(true);
            if (err != null || dto == null) {
                Toast.makeText(requireContext(), getString(R.string.doctor_panel_action_failed_fmt, err != null ? err : ""), Toast.LENGTH_LONG).show();
                return;
            }
            bound = dto;
            if (!silent) {
                Toast.makeText(requireContext(), R.string.doctor_exam_notes_saved, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmComplete() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.doctor_exam_complete_confirm)
                .setPositiveButton(android.R.string.ok, (d, w) -> onComplete())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onComplete() {
        int id = appointmentIdOrZero();
        String auth = bearer();
        if (id <= 0 || auth == null) return;
        String note = notesField.getText() != null ? notesField.getText().toString().trim() : "";
        setButtonsEnabled(false);
        repository.completeExam(auth, id, note, (dto, err) -> {
            if (!isAdded()) return;
            setButtonsEnabled(true);
            if (err != null || dto == null) {
                Toast.makeText(requireContext(), getString(R.string.doctor_panel_action_failed_fmt, err != null ? err : ""), Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(requireContext(), R.string.doctor_exam_completed_toast, Toast.LENGTH_SHORT).show();
            focusAppointmentId = 0;
            bindDetail(dto);
        });
    }

    private void confirmNoShow() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.doctor_exam_no_show_confirm)
                .setPositiveButton(android.R.string.ok, (d, w) -> onNoShow())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onNoShow() {
        int id = appointmentIdOrZero();
        String auth = bearer();
        if (id <= 0 || auth == null) return;
        setButtonsEnabled(false);
        repository.markNoShow(auth, id, (dto, err) -> {
            if (!isAdded()) return;
            setButtonsEnabled(true);
            if (err != null || dto == null) {
                Toast.makeText(requireContext(), getString(R.string.doctor_panel_action_failed_fmt, err != null ? err : ""), Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(requireContext(), R.string.doctor_exam_no_show_toast, Toast.LENGTH_SHORT).show();
            focusAppointmentId = 0;
            showEmptyState();
            resolveAndLoad();
        });
    }
}
