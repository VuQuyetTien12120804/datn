package com.example.frontend_bookingcare.ui.doctor_panel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DoctorRequestsFragment extends Fragment {

    enum ReqStatus { PENDING, ACCEPTED, REJECTED }

    static class DoctorReq {
        final String name;
        final int age;
        final String gender;
        final String date;
        final String time;
        final String reason;
        final boolean priority;
        ReqStatus status;

        DoctorReq(String name, int age, String gender, String date, String time, String reason, boolean priority, ReqStatus status) {
            this.name = name;
            this.age = age;
            this.gender = gender;
            this.date = date;
            this.time = time;
            this.reason = reason;
            this.priority = priority;
            this.status = status;
        }
    }

    private final List<DoctorReq> allRequests = seed();
    private ReqStatus selected = ReqStatus.PENDING;
    private ReqAdapter adapter;
    private TextView tabPending, tabAccepted, tabRejected;
    private TextView subtitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DoctorUiHelper.applyTopInsetPadding(view, 12);
        subtitle = view.findViewById(R.id.doctor_req_subtitle);
        tabPending = view.findViewById(R.id.doctor_tab_pending);
        tabAccepted = view.findViewById(R.id.doctor_tab_accepted);
        tabRejected = view.findViewById(R.id.doctor_tab_rejected);

        RecyclerView list = view.findViewById(R.id.doctor_req_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReqAdapter();
        list.setAdapter(adapter);

        tabPending.setOnClickListener(v -> selectTab(ReqStatus.PENDING));
        tabAccepted.setOnClickListener(v -> selectTab(ReqStatus.ACCEPTED));
        tabRejected.setOnClickListener(v -> selectTab(ReqStatus.REJECTED));

        refresh();
    }

    private void selectTab(ReqStatus s) {
        selected = s;
        refresh();
    }

    private void refresh() {
        int pending = 0, accepted = 0, rejected = 0;
        for (DoctorReq r : allRequests) {
            if (r.status == ReqStatus.PENDING) pending++;
            else if (r.status == ReqStatus.ACCEPTED) accepted++;
            else rejected++;
        }
        tabPending.setText(getString(R.string.doctor_tab_pending) + "   " + pending);
        tabAccepted.setText(getString(R.string.doctor_tab_accepted) + "   " + accepted);
        tabRejected.setText(getString(R.string.doctor_tab_rejected) + "   " + rejected);

        tabPending.setBackgroundResource(selected == ReqStatus.PENDING ? R.drawable.bg_doctor_tab_active : R.drawable.bg_doctor_tab_inactive);
        tabAccepted.setBackgroundResource(selected == ReqStatus.ACCEPTED ? R.drawable.bg_doctor_tab_active : R.drawable.bg_doctor_tab_inactive);
        tabRejected.setBackgroundResource(selected == ReqStatus.REJECTED ? R.drawable.bg_doctor_tab_active : R.drawable.bg_doctor_tab_inactive);
        int active = getResources().getColor(R.color.doctor_current_card_stroke, null);
        int normal = getResources().getColor(R.color.text_primary, null);
        tabPending.setTextColor(selected == ReqStatus.PENDING ? active : normal);
        tabAccepted.setTextColor(selected == ReqStatus.ACCEPTED ? active : normal);
        tabRejected.setTextColor(selected == ReqStatus.REJECTED ? active : normal);

        subtitle.setText(getString(R.string.doctor_requests_pending_fmt, pending));
        adapter.submit(filter());
    }

    private List<DoctorReq> filter() {
        List<DoctorReq> out = new ArrayList<>();
        for (DoctorReq r : allRequests) if (r.status == selected) out.add(r);
        return out;
    }

    private List<DoctorReq> seed() {
        return new ArrayList<>(Arrays.asList(
                new DoctorReq("Đỗ Thị Mai",     35, "Nữ",  "03/02/2026", "09:00 AM", "Đau đầu dai dẳng, chóng mặt khi đứng lên",  true,  ReqStatus.PENDING),
                new DoctorReq("Ngô Văn Tùng",   42, "Nam", "04/02/2026", "10:30 AM", "Kiểm tra huyết áp định kỳ",                  false, ReqStatus.PENDING),
                new DoctorReq("Phạm Thị Diễm",  29, "Nữ",  "05/02/2026", "14:00 PM", "Đau bụng, khó tiêu sau ăn",                  false, ReqStatus.PENDING),
                new DoctorReq("Lê Thanh Hà",    51, "Nữ",  "06/02/2026", "08:30 AM", "Ho khan kéo dài",                             true,  ReqStatus.PENDING),
                new DoctorReq("Hoàng Minh",     33, "Nam", "01/02/2026", "15:00 PM", "Khám sức khỏe tổng quát",                    false, ReqStatus.ACCEPTED),
                new DoctorReq("Bùi Khánh Linh", 27, "Nữ",  "01/02/2026", "16:00 PM", "Tư vấn dinh dưỡng",                          false, ReqStatus.ACCEPTED),
                new DoctorReq("Trần Quốc Bảo",  60, "Nam", "30/01/2026", "10:00 AM", "Đau lưng mãn tính",                          false, ReqStatus.REJECTED),
                new DoctorReq("Vũ Thị Hương",   38, "Nữ",  "29/01/2026", "11:30 AM", "Mất ngủ",                                     false, ReqStatus.REJECTED)
        ));
    }

    class ReqAdapter extends RecyclerView.Adapter<ReqAdapter.H> {
        final List<DoctorReq> items = new ArrayList<>();

        void submit(List<DoctorReq> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public H onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor_request, parent, false);
            return new H(v);
        }

        @Override
        public void onBindViewHolder(@NonNull H h, int pos) {
            DoctorReq r = items.get(pos);
            h.name.setText(r.name);
            h.ageGender.setText(getString(R.string.doctor_age_gender_fmt, r.age, r.gender));
            h.date.setText(getString(R.string.doctor_request_date_fmt, r.date));
            h.time.setText(getString(R.string.doctor_request_time_fmt, r.time));
            h.reason.setText(getString(R.string.doctor_request_reason_fmt, r.reason));
            h.priorityRow.setVisibility(r.priority ? View.VISIBLE : View.GONE);

            if (r.status == ReqStatus.PENDING) {
                h.actionRow.setVisibility(View.VISIBLE);
                h.btnAccept.setOnClickListener(v -> {
                    r.status = ReqStatus.ACCEPTED;
                    refresh();
                });
                h.btnReject.setOnClickListener(v -> {
                    r.status = ReqStatus.REJECTED;
                    refresh();
                });
            } else {
                h.actionRow.setVisibility(View.GONE);
            }

            if (r.priority && r.status == ReqStatus.PENDING) {
                h.root.setStrokeWidth((int) (1 * h.itemView.getResources().getDisplayMetrics().density));
                h.root.setStrokeColor(h.itemView.getResources().getColor(R.color.doctor_priority_orange, null));
            } else {
                h.root.setStrokeWidth(0);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class H extends RecyclerView.ViewHolder {
            final com.google.android.material.card.MaterialCardView root;
            final TextView name, ageGender, date, time, reason, btnAccept, btnReject;
            final View priorityRow, actionRow;

            H(@NonNull View itemView) {
                super(itemView);
                root = (com.google.android.material.card.MaterialCardView) itemView;
                name = itemView.findViewById(R.id.req_name);
                ageGender = itemView.findViewById(R.id.req_age_gender);
                date = itemView.findViewById(R.id.req_date);
                time = itemView.findViewById(R.id.req_time);
                reason = itemView.findViewById(R.id.req_reason);
                btnAccept = itemView.findViewById(R.id.req_btn_accept);
                btnReject = itemView.findViewById(R.id.req_btn_reject);
                priorityRow = itemView.findViewById(R.id.req_priority_row);
                actionRow = itemView.findViewById(R.id.req_action_row);
            }
        }
    }
}
