package com.example.frontend_bookingcare.ui.doctor_panel;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.DoctorAppointmentDto;
import com.example.frontend_bookingcare.data.DoctorPanelRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DoctorAppointmentsFragment extends Fragment {

    enum ApptStatus { DONE, PROGRESS, WAIT }

    static class DoctorAppt {
        final String time;
        final String patient;
        final String symptom;
        final ApptStatus status;

        DoctorAppt(String time, String patient, String symptom, ApptStatus status) {
            this.time = time;
            this.patient = patient;
            this.symptom = symptom;
            this.status = status;
        }
    }

    private final DoctorPanelRepository repository = new DoctorPanelRepository();
    private ApptAdapter apptAdapter;
    private TextView countView;
    private SwipeRefreshLayout refresh;
    private View emptyView;
    private RecyclerView listView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefresh = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;
            loadToday(false);
            handler.postDelayed(this, 10_000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_appointments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DoctorUiHelper.applyTopInsetPadding(view, 12);
        bindHeader(view);

        refresh = view.findViewById(R.id.doctor_appt_refresh);
        refresh.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.doctor_brand_primary),
                ContextCompat.getColor(requireContext(), R.color.home_tile_blue));
        refresh.setOnRefreshListener(() -> loadToday(true));

        countView = view.findViewById(R.id.doctor_appt_count);
        emptyView = view.findViewById(R.id.doctor_appt_empty);
        listView = view.findViewById(R.id.doctor_appt_list);
        listView.setLayoutManager(new LinearLayoutManager(requireContext()));
        apptAdapter = new ApptAdapter(new ArrayList<>());
        listView.setAdapter(apptAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadToday(true);
        handler.removeCallbacks(autoRefresh);
        handler.postDelayed(autoRefresh, 10_000);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(autoRefresh);
    }

    private void bindHeader(@NonNull View view) {
        TextView date = view.findViewById(R.id.doctor_appt_date);
        SimpleDateFormat fmt = new SimpleDateFormat("EEEE, d 'tháng' M, yyyy", new Locale("vi"));
        String s = fmt.format(Calendar.getInstance().getTime());
        date.setText(Character.toUpperCase(s.charAt(0)) + s.substring(1));

        TextView shift = view.findViewById(R.id.doctor_appt_shift);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        shift.setText(getString(hour < 12 ? R.string.doctor_work_shift_morning : R.string.doctor_work_shift_afternoon));
    }

    private void loadToday(boolean showSpinner) {
        if (countView == null || apptAdapter == null) return;
        if (refresh != null) {
            refresh.setRefreshing(showSpinner);
        }
        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            Toast.makeText(requireContext(), R.string.doctor_panel_session_required, Toast.LENGTH_SHORT).show();
            apptAdapter.submit(new ArrayList<>());
            countView.setText(getString(R.string.doctor_today_count_fmt, 0));
            toggleScheduleEmpty(true);
            if (refresh != null) refresh.setRefreshing(false);
            return;
        }
        repository.fetchToday("Bearer " + s.accessToken, (list, err) -> {
            if (!isAdded()) return;
            if (refresh != null) refresh.setRefreshing(false);
            if (err != null) {
                Toast.makeText(requireContext(), getString(R.string.doctor_panel_load_failed_fmt, err), Toast.LENGTH_LONG).show();
                apptAdapter.submit(new ArrayList<>());
                countView.setText(getString(R.string.doctor_today_count_fmt, 0));
                toggleScheduleEmpty(true);
                return;
            }
            List<DoctorAppt> mapped = mapToday(list != null ? list : new ArrayList<>());
            apptAdapter.submit(mapped);
            countView.setText(getString(R.string.doctor_today_count_fmt, mapped.size()));
            toggleScheduleEmpty(mapped.isEmpty());
        });
    }

    private void toggleScheduleEmpty(boolean empty) {
        if (emptyView != null) emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (listView != null) listView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private static List<DoctorAppt> mapToday(List<DoctorAppointmentDto> list) {
        List<DoctorAppt> out = new ArrayList<>();
        for (DoctorAppointmentDto d : list) {
            String time = DoctorPanelFormatters.formatTimeAmPm(d.expectedTime);
            String patient = TextUtils.isEmpty(d.patientName) ? "—" : d.patientName;
            String symptom = TextUtils.isEmpty(d.notes) ? "—" : d.notes;
            out.add(new DoctorAppt(time, patient, symptom, mapApptStatus(d.status)));
        }
        return out;
    }

    private static ApptStatus mapApptStatus(String status) {
        if (status == null) return ApptStatus.WAIT;
        switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "COMPLETED":
                return ApptStatus.DONE;
            case "CHECKED_IN":
            case "IN_PROGRESS":
                return ApptStatus.PROGRESS;
            default:
                return ApptStatus.WAIT;
        }
    }

    static class ApptAdapter extends RecyclerView.Adapter<ApptAdapter.H> {
        final List<DoctorAppt> items = new ArrayList<>();

        ApptAdapter(List<DoctorAppt> initial) {
            items.addAll(initial);
        }

        void submit(List<DoctorAppt> next) {
            items.clear();
            items.addAll(next);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public H onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor_appointment, parent, false);
            return new H(v);
        }

        @Override
        public void onBindViewHolder(@NonNull H holder, int pos) {
            DoctorAppt a = items.get(pos);
            holder.time.setText(a.time);
            holder.patient.setText(a.patient);
            holder.symptom.setText(holder.itemView.getContext().getString(R.string.doctor_symptom_fmt, a.symptom));
            DoctorPanelUi.stylePatientAvatar(holder.initial, a.patient, holder.itemView.getContext());
            if (holder.timelineLine != null) {
                holder.timelineLine.setVisibility(pos == getItemCount() - 1 ? View.GONE : View.VISIBLE);
            }

            int statusBg, statusFg, statusText;
            switch (a.status) {
                case DONE:
                    statusBg = R.drawable.bg_doctor_status_done;
                    statusFg = R.color.doctor_status_done_fg;
                    statusText = R.string.doctor_status_done;
                    break;
                case PROGRESS:
                    statusBg = R.drawable.bg_doctor_status_progress;
                    statusFg = R.color.doctor_status_progress_fg;
                    statusText = R.string.doctor_status_progress;
                    break;
                default:
                    statusBg = R.drawable.bg_doctor_status_wait;
                    statusFg = R.color.doctor_status_wait_fg;
                    statusText = R.string.doctor_status_wait;
            }
            holder.status.setBackgroundResource(statusBg);
            holder.status.setText(statusText);
            holder.status.setTextColor(holder.itemView.getResources().getColor(statusFg, null));

            if (a.status == ApptStatus.PROGRESS) {
                holder.card.setStrokeWidth((int) (2 * holder.itemView.getResources().getDisplayMetrics().density));
                holder.card.setStrokeColor(holder.itemView.getResources().getColor(R.color.doctor_current_card_stroke, null));
            } else {
                holder.card.setStrokeWidth(0);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class H extends RecyclerView.ViewHolder {
            final TextView time, status, patient, symptom, initial;
            final View timelineLine;
            final com.google.android.material.card.MaterialCardView card;

            H(@NonNull View itemView) {
                super(itemView);
                time = itemView.findViewById(R.id.appt_time);
                status = itemView.findViewById(R.id.appt_status);
                patient = itemView.findViewById(R.id.appt_patient);
                symptom = itemView.findViewById(R.id.appt_symptom);
                initial = itemView.findViewById(R.id.appt_initial);
                timelineLine = itemView.findViewById(R.id.appt_timeline_line);
                card = itemView.findViewById(R.id.appt_card);
            }
        }
    }
}
