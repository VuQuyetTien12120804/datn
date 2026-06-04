package com.example.frontend_bookingcare.ui.appointments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.PatientAppointmentDto;
import com.example.frontend_bookingcare.data.PatientAppointmentsRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.booking.BookingFormatters;
import com.example.frontend_bookingcare.ui.common.HeaderInsets;
import com.example.frontend_bookingcare.ui.common.PatientEmptyUi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class AppointmentsFragment extends Fragment {

    private LinearLayout cards;
    private View emptyState;
    private SwipeRefreshLayout refresh;
    private TabLayout tabs;
    private final PatientAppointmentsRepository repository = new PatientAppointmentsRepository();
    private int selectedTab = 0;
    private List<PatientAppointmentDto> last = new ArrayList<>();
    private ActivityResultLauncher<Intent> ticketLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Apply safe-area to toolbar so blue covers status bar area (no white gap).
        MaterialToolbar tb = view.findViewById(R.id.appointments_toolbar);
        HeaderInsets.applyToToolbar(tb);

        cards = view.findViewById(R.id.appointments_cards);
        emptyState = view.findViewById(R.id.appointments_empty);
        refresh = view.findViewById(R.id.appointments_refresh);
        tabs = view.findViewById(R.id.appointments_tabs);
        tabs.addTab(tabs.newTab().setText(R.string.tab_upcoming));
        tabs.addTab(tabs.newTab().setText(R.string.tab_completed));
        tabs.addTab(tabs.newTab().setText(R.string.tab_cancelled));

        ticketLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                load();
            }
        });

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                load();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        refresh.setOnRefreshListener(this::load);
    }

    // toolbar insets handled by HeaderInsets

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    /**
     * MainActivity đổi tab bằng hide/show — onResume không chạy lại khi quay lại tab này.
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            load();
        }
    }

    public void reloadForSessionChange() {
        last.clear();
        if (isAdded()) load();
    }

    private void load() {
        if (!isAdded()) return;
        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            refresh.setRefreshing(false);
            fillCards(new ArrayList<>(), getString(R.string.booking_need_login));
            return;
        }
        String group = selectedTab == 1 ? "COMPLETED" : (selectedTab == 2 ? "CANCELLED" : "UPCOMING");
        refresh.setRefreshing(true);
        repository.fetch("Bearer " + s.accessToken, group, (list, err) -> {
            if (!isAdded()) return;
            refresh.setRefreshing(false);
            if (err != null) {
                fillCards(new ArrayList<>(), err);
                return;
            }
            last = list != null ? list : new ArrayList<>();
            fillCards(last, null);
        });
    }

    private void fillCards(List<PatientAppointmentDto> list, @Nullable String errorOrHint) {
        cards.removeAllViews();

        if (errorOrHint != null) {
            cards.setVisibility(View.GONE);
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
                if (getString(R.string.booking_need_login).equals(errorOrHint)) {
                    PatientEmptyUi.bind(emptyState, android.R.drawable.ic_lock_lock,
                            R.string.messages_login_title, R.string.booking_need_login);
                } else {
                    PatientEmptyUi.bindMessage(emptyState, R.string.appointments_error_title, errorOrHint);
                }
            }
            return;
        }

        if (list == null || list.isEmpty()) {
            cards.setVisibility(View.GONE);
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
                PatientEmptyUi.bind(emptyState, android.R.drawable.ic_menu_my_calendar,
                        R.string.appointments_empty_title, R.string.appointments_empty_hint);
            }
            return;
        }

        if (emptyState != null) emptyState.setVisibility(View.GONE);
        cards.setVisibility(View.VISIBLE);

        LayoutInflater inf = LayoutInflater.from(requireContext());
        SessionManager sm = new SessionManager(requireContext());
        String patientName = sm.getSession() != null && !TextUtils.isEmpty(sm.getSession().fullName) ? sm.getSession().fullName : "—";

        for (PatientAppointmentDto a : list) {
            View v = inf.inflate(R.layout.item_appointment_card, cards, false);
            TextView name = v.findViewById(R.id.appt_doctor_name);
            TextView spec = v.findViewById(R.id.appt_specialty);
            TextView time = v.findViewById(R.id.appt_time);
            TextView st = v.findViewById(R.id.appt_status_text);
            TextView stt = v.findViewById(R.id.appt_stt);
            TextView p = v.findViewById(R.id.appt_patient);
            TextView avatar = v.findViewById(R.id.appt_doctor_avatar);
            View accent = v.findViewById(R.id.appt_accent_bar);
            v.findViewById(R.id.appt_cancel).setVisibility(View.GONE);
            v.findViewById(R.id.appt_detail).setVisibility(View.GONE);

            String doctorName = a.doctorName != null ? a.doctorName : "—";
            name.setText(doctorName);
            AppointmentCardUi.bindDoctorAvatar(avatar, doctorName, requireContext());
            if (!TextUtils.isEmpty(a.specialty)) {
                spec.setText(a.specialty);
                spec.setVisibility(View.VISIBLE);
            } else {
                spec.setVisibility(View.GONE);
            }
            String lineTime = BookingFormatters.timeRange(a.startTime, a.endTime);
            String lineDate = BookingFormatters.prettyDate(requireContext(), a.appointmentDate);
            time.setText(lineTime + " · " + lineDate);
            if (a.queueNumber != null && a.queueNumber > 0) {
                stt.setText(getString(R.string.appt_queue_fmt, a.queueNumber));
            } else {
                stt.setText(AppointmentCodes.appointmentCode(
                        a.appointmentId != null ? a.appointmentId : 0, a.appointmentDate));
            }
            p.setText(getString(R.string.appt_patient_fmt, patientName));
            String tabText = statusDisplay(a.status, selectedTab);
            st.setText(tabText);
            AppointmentCardUi.bindStatus(st, accent, a.status, requireContext());
            cards.addView(v);

            v.setOnClickListener(vv -> {
                int apptId = a.appointmentId != null ? a.appointmentId : 0;
                String docName = a.doctorName != null ? a.doctorName : "";
                String d = a.appointmentDate != null ? a.appointmentDate : "";
                String stTime = a.startTime != null ? a.startTime : "";
                String enTime = a.endTime != null ? a.endTime : "";
                String status = a.status != null ? a.status : "";
                int docId = a.doctorId != null ? a.doctorId : 0;
                if (ticketLauncher != null) {
                    ticketLauncher.launch(AppointmentTicketActivity.newIntent(
                            requireContext(),
                            apptId,
                            docId,
                            docName,
                            d,
                            stTime,
                            enTime,
                            status
                    ));
                } else {
                    startActivity(AppointmentTicketActivity.newIntent(
                            requireContext(),
                            apptId,
                            docId,
                            docName,
                            d,
                            stTime,
                            enTime,
                            status
                    ));
                }
            });
        }
    }

    private String statusDisplay(@Nullable String status, int tab) {
        if (status == null || status.trim().isEmpty()) {
            return tab == 1 ? getString(R.string.tab_completed) : (tab == 2 ? getString(R.string.tab_cancelled) : getString(R.string.tab_upcoming));
        }
        switch (status.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "PENDING":
                return getString(R.string.appt_status_booked);
            case "CONFIRMED":
                return getString(R.string.appt_status_confirmed);
            case "CHECKED_IN":
                return getString(R.string.appt_status_checked_in);
            case "COMPLETED":
                return getString(R.string.appt_status_completed);
            case "CANCELLED":
                return getString(R.string.appt_status_cancelled);
            case "NO_SHOW":
                return getString(R.string.appt_status_no_show);
            default:
                return status;
        }
    }
}
