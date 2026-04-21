package com.example.frontend_bookingcare.ui.doctor_panel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;

import java.text.SimpleDateFormat;
import java.util.Arrays;
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

        RecyclerView list = view.findViewById(R.id.doctor_appt_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<DoctorAppt> data = demoData();
        ((TextView) view.findViewById(R.id.doctor_appt_count))
                .setText(getString(R.string.doctor_today_count_fmt, data.size()));
        list.setAdapter(new ApptAdapter(data));
    }

    private void bindHeader(@NonNull View view) {
        TextView date = view.findViewById(R.id.doctor_appt_date);
        SimpleDateFormat fmt = new SimpleDateFormat("EEEE, d 'tháng' M, yyyy", new Locale("vi"));
        String s = fmt.format(Calendar.getInstance().getTime());
        date.setText(Character.toUpperCase(s.charAt(0)) + s.substring(1));

        TextView shift = view.findViewById(R.id.doctor_appt_shift);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        shift.setText(hour < 12 ? R.string.doctor_work_shift_morning : R.string.doctor_work_shift_afternoon);
    }

    private List<DoctorAppt> demoData() {
        return Arrays.asList(
                new DoctorAppt("09:00 AM", "Nguyễn Văn An",  "Đau đầu, sốt nhẹ",      ApptStatus.DONE),
                new DoctorAppt("09:30 AM", "Trần Thị Bình",  "Ho khan, khó thở",       ApptStatus.PROGRESS),
                new DoctorAppt("10:00 AM", "Lê Minh Cường",  "Khám tổng quát định kỳ", ApptStatus.WAIT),
                new DoctorAppt("10:30 AM", "Phạm Thị Diễm",  "Đau bụng, khó tiêu",     ApptStatus.WAIT),
                new DoctorAppt("11:00 AM", "Đỗ Thị Mai",     "Đau đầu dai dẳng",       ApptStatus.WAIT),
                new DoctorAppt("11:30 AM", "Ngô Văn Tùng",   "Kiểm tra huyết áp",      ApptStatus.WAIT)
        );
    }

    static class ApptAdapter extends RecyclerView.Adapter<ApptAdapter.H> {
        final List<DoctorAppt> items;

        ApptAdapter(List<DoctorAppt> items) { this.items = items; }

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
            holder.dot.setBackgroundResource(statusBg);

            if (a.status == ApptStatus.PROGRESS) {
                holder.card.setStrokeWidth((int) (2 * holder.itemView.getResources().getDisplayMetrics().density));
                holder.card.setStrokeColor(holder.itemView.getResources().getColor(R.color.doctor_current_card_stroke, null));
            } else {
                holder.card.setStrokeWidth(0);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class H extends RecyclerView.ViewHolder {
            final TextView time, status, patient, symptom;
            final FrameLayout dot;
            final com.google.android.material.card.MaterialCardView card;

            H(@NonNull View itemView) {
                super(itemView);
                time = itemView.findViewById(R.id.appt_time);
                status = itemView.findViewById(R.id.appt_status);
                patient = itemView.findViewById(R.id.appt_patient);
                symptom = itemView.findViewById(R.id.appt_symptom);
                dot = itemView.findViewById(R.id.appt_dot);
                card = itemView.findViewById(R.id.appt_card);
            }
        }
    }
}
