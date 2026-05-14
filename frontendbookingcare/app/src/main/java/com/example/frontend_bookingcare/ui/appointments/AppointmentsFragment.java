package com.example.frontend_bookingcare.ui.appointments;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;

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
import com.example.frontend_bookingcare.ui.common.HeaderInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class AppointmentsFragment extends Fragment {

    private LinearLayout cards;
    private SwipeRefreshLayout refresh;
    private TabLayout tabs;
    private EditText search;
    private final PatientAppointmentsRepository repository = new PatientAppointmentsRepository();
    private int selectedTab = 0;
    private String query = "";
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
        refresh = view.findViewById(R.id.appointments_refresh);
        tabs = view.findViewById(R.id.appointments_tabs);
        search = view.findViewById(R.id.appointments_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s != null ? s.toString().trim() : "";
                fillCards(last, null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
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
        load();
    }

    // toolbar insets handled by HeaderInsets

    @Override
    public void onResume() {
        super.onResume();
        load();
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
        LayoutInflater inf = LayoutInflater.from(requireContext());
        SessionManager sm = new SessionManager(requireContext());
        String patientName = sm.getSession() != null && !TextUtils.isEmpty(sm.getSession().fullName) ? sm.getSession().fullName : "—";

        if (errorOrHint != null) {
            View v = inf.inflate(R.layout.item_appointment_card, cards, false);
            TextView name = v.findViewById(R.id.appt_doctor_name);
            TextView spec = v.findViewById(R.id.appt_specialty);
            TextView time = v.findViewById(R.id.appt_time);
            TextView st = v.findViewById(R.id.appt_status_text);
            TextView stt = v.findViewById(R.id.appt_stt);
            TextView p = v.findViewById(R.id.appt_patient);
            v.findViewById(R.id.appt_cancel).setVisibility(View.GONE);
            v.findViewById(R.id.appt_detail).setVisibility(View.GONE);
            name.setText(getString(R.string.appointments_title));
            spec.setText("");
            stt.setText("");
            time.setText(errorOrHint);
            p.setText("");
            st.setText(selectedTab == 1 ? getString(R.string.tab_completed) : (selectedTab == 2 ? getString(R.string.tab_cancelled) : getString(R.string.tab_upcoming)));
            cards.addView(v);
            return;
        }

        if (list == null || list.isEmpty()) {
            View v = inf.inflate(R.layout.item_appointment_card, cards, false);
            TextView name = v.findViewById(R.id.appt_doctor_name);
            TextView spec = v.findViewById(R.id.appt_specialty);
            TextView time = v.findViewById(R.id.appt_time);
            TextView stt = v.findViewById(R.id.appt_stt);
            TextView p = v.findViewById(R.id.appt_patient);
            v.findViewById(R.id.appt_cancel).setVisibility(View.GONE);
            v.findViewById(R.id.appt_detail).setVisibility(View.GONE);
            name.setText(getString(R.string.appointments_title));
            spec.setText("");
            stt.setText("");
            time.setText(getString(R.string.booking_not_available));
            p.setText("");
            cards.addView(v);
            return;
        }

        for (PatientAppointmentDto a : list) {
            if (!TextUtils.isEmpty(query)) {
                String needle = query.toLowerCase();
                String hay = (a.doctorName != null ? a.doctorName : "") + " " + patientName + " " + (a.appointmentDate != null ? a.appointmentDate : "") + " " + (a.appointmentId != null ? a.appointmentId : "");
                if (!hay.toLowerCase().contains(needle)) continue;
            }

            View v = inf.inflate(R.layout.item_appointment_card, cards, false);
            TextView name = v.findViewById(R.id.appt_doctor_name);
            TextView spec = v.findViewById(R.id.appt_specialty);
            TextView time = v.findViewById(R.id.appt_time);
            TextView st = v.findViewById(R.id.appt_status_text);
            TextView stt = v.findViewById(R.id.appt_stt);
            TextView p = v.findViewById(R.id.appt_patient);
            v.findViewById(R.id.appt_cancel).setVisibility(View.GONE);
            v.findViewById(R.id.appt_detail).setVisibility(View.GONE);

            name.setText(a.doctorName != null ? a.doctorName : "—");
            spec.setText(""); // backend chưa trả specialty trong endpoint này
            String lineTime = (a.startTime != null ? a.startTime : "") + (a.endTime != null && !TextUtils.isEmpty(a.endTime) ? ("-" + a.endTime) : "");
            String lineDate = a.appointmentDate != null ? a.appointmentDate : "";
            time.setText(lineTime + " - " + lineDate);
            stt.setText("STT " + (a.appointmentId != null ? a.appointmentId : "—"));
            p.setText("Bệnh nhân: " + patientName);
            String tabText = selectedTab == 1 ? getString(R.string.tab_completed) : (selectedTab == 2 ? getString(R.string.tab_cancelled) : getString(R.string.tab_upcoming));
            st.setText(tabText);
            cards.addView(v);

            v.setOnClickListener(vv -> {
                int apptId = a.appointmentId != null ? a.appointmentId : 0;
                String doctorName = a.doctorName != null ? a.doctorName : "";
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
                            doctorName,
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
                            doctorName,
                            d,
                            stTime,
                            enTime,
                            status
                    ));
                }
            });
        }
    }
}
