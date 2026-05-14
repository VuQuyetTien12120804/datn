package com.example.frontend_bookingcare.ui.doctor_panel;

import android.os.Bundle;
import android.content.Context;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.DoctorAppointmentDetailDto;
import com.example.frontend_bookingcare.api.DoctorAppointmentDto;
import com.example.frontend_bookingcare.data.DoctorPanelRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class DoctorRequestsFragment extends Fragment {

    enum ReqStatus { PENDING, ACCEPTED, REJECTED }

    private interface ReqListRow {
    }

    private static final class SectionHeaderRow implements ReqListRow {
        final String title;
        final String subtitle;
        final boolean overdueStyle;

        SectionHeaderRow(String title, String subtitle, boolean overdueStyle) {
            this.title = title;
            this.subtitle = subtitle;
            this.overdueStyle = overdueStyle;
        }
    }

    private static final class ItemRow implements ReqListRow {
        final DoctorReq req;

        ItemRow(DoctorReq req) {
            this.req = req;
        }
    }

    static class DoctorReq {
        final int appointmentId;
        final String name;
        int age;
        String gender;
        final String date;
        final String time;
        final String reason;
        final boolean priority;
        final ReqStatus tab;
        @Nullable final String appointmentDateIso;
        @Nullable final Long slotStartMillis;
        /** Trạng thái API gốc (để biết đã khám xong / đang khám). */
        @Nullable final String rawStatus;

        DoctorReq(int appointmentId, String name, int age, String gender, String date, String time,
                  String reason, boolean priority, ReqStatus tab,
                  @Nullable String appointmentDateIso, @Nullable Long slotStartMillis,
                  @Nullable String rawStatus) {
            this.appointmentId = appointmentId;
            this.name = name;
            this.age = age;
            this.gender = gender;
            this.date = date;
            this.time = time;
            this.reason = reason;
            this.priority = priority;
            this.tab = tab;
            this.appointmentDateIso = appointmentDateIso;
            this.slotStartMillis = slotStartMillis;
            this.rawStatus = rawStatus;
        }

        boolean isPastSlot() {
            long now = System.currentTimeMillis();
            if (slotStartMillis != null) {
                return slotStartMillis < now;
            }
            return DoctorPanelFormatters.isStrictlyPastAppointmentDay(appointmentDateIso);
        }

        /**
         * Đã chấp nhận nhưng đã qua giờ/ngày hẹn và chưa ghi nhận khám (theo trạng thái API).
         */
        boolean isMissedAcceptedExam() {
            if (tab != ReqStatus.ACCEPTED) return false;
            if (!isPastSlot()) return false;
            if (rawStatus == null) return true;
            String u = rawStatus.trim().toUpperCase(Locale.ROOT);
            if ("COMPLETED".equals(u)) return false;
            if ("IN_PROGRESS".equals(u)) return false;
            if ("CHECKED_IN".equals(u)) return false;
            return true;
        }

        static DoctorReq from(DoctorAppointmentDto d, ReqStatus tab) {
            int id = d.appointmentId != null ? d.appointmentId : -1;
            String name = TextUtils.isEmpty(d.patientName) ? "—" : d.patientName;
            int age = DoctorPanelFormatters.resolvePatientAge(d.dob, d.patientAge);
            String gender = DoctorPanelFormatters.displayGender(d.gender);
            String date = DoctorPanelFormatters.formatAppointmentDate(d.appointmentDate);
            String time = DoctorPanelFormatters.formatTimeAmPm(d.expectedTime);
            String reason = TextUtils.isEmpty(d.notes) ? "—" : d.notes;
            String iso = d.appointmentDate != null ? d.appointmentDate.trim() : null;
            if (TextUtils.isEmpty(iso)) {
                iso = null;
            }
            Long slotMs = DoctorPanelFormatters.appointmentSlotStartMillis(d.appointmentDate, d.expectedTime);
            String st = d.status != null ? d.status.trim() : null;
            if (TextUtils.isEmpty(st)) st = null;
            return new DoctorReq(id, name, age, gender, date, time, reason, false, tab, iso, slotMs, st);
        }
    }

    private final List<DoctorReq> pendingItems = new ArrayList<>();
    private final List<DoctorReq> acceptedItems = new ArrayList<>();
    private final List<DoctorReq> rejectedItems = new ArrayList<>();

    private ReqStatus selected = ReqStatus.PENDING;
    private ReqAdapter adapter;
    private TextView tabPending, tabAccepted, tabRejected;
    private TextView subtitle;

    private View reqEmpty;
    private RecyclerView reqList;
    private View tabLegendCard;
    private TextView tabLegendText;

    private final DoctorPanelRepository repository = new DoctorPanelRepository();

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
        MaterialButtonToggleGroup tabs = view.findViewById(R.id.doctor_req_tabs);
        MaterialButton btnPending = view.findViewById(R.id.doctor_tab_pending);
        MaterialButton btnAccepted = view.findViewById(R.id.doctor_tab_accepted);
        MaterialButton btnRejected = view.findViewById(R.id.doctor_tab_rejected);
        tabPending = btnPending;
        tabAccepted = btnAccepted;
        tabRejected = btnRejected;

        reqList = view.findViewById(R.id.doctor_req_list);
        reqEmpty = view.findViewById(R.id.doctor_req_empty);
        tabLegendCard = view.findViewById(R.id.doctor_req_tab_legend);
        tabLegendText = view.findViewById(R.id.doctor_req_legend_text);
        reqList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReqAdapter();
        reqList.setAdapter(adapter);

        tabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.doctor_tab_pending) {
                selectTab(ReqStatus.PENDING);
            } else if (checkedId == R.id.doctor_tab_accepted) {
                selectTab(ReqStatus.ACCEPTED);
            } else if (checkedId == R.id.doctor_tab_rejected) {
                selectTab(ReqStatus.REJECTED);
            }
        });
        // default selection
        tabs.check(R.id.doctor_tab_pending);

        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAppointmentTabs();
    }

    private void selectTab(ReqStatus s) {
        selected = s;
        refresh();
    }

    private void refresh() {
        int actionable = actionablePendingCount();
        int overdue = countOverduePending();
        int accepted = acceptedItems.size();
        int acceptedMissed = countMissedAccepted();
        int rejected = rejectedItems.size();

        tabPending.setText(getString(R.string.doctor_tab_pending) + "   " + actionable);
        tabAccepted.setText(getString(R.string.doctor_tab_accepted) + "   " + accepted);
        tabRejected.setText(getString(R.string.doctor_tab_rejected) + "   " + rejected);

        // Keep button state in sync (Material toggle group handles visuals)
        if (selected == ReqStatus.PENDING) {
            ((MaterialButton) tabPending).setChecked(true);
        } else if (selected == ReqStatus.ACCEPTED) {
            ((MaterialButton) tabAccepted).setChecked(true);
        } else {
            ((MaterialButton) tabRejected).setChecked(true);
        }

        switch (selected) {
            case PENDING: {
                if (overdue > 0) {
                    subtitle.setText(getString(R.string.doctor_requests_subtitle_pending_with_overdue_fmt,
                            actionable, overdue));
                } else {
                    subtitle.setText(getString(R.string.doctor_requests_subtitle_pending_fmt, actionable));
                }
                break;
            }
            case ACCEPTED:
                if (acceptedMissed > 0) {
                    int upcoming = accepted - acceptedMissed;
                    subtitle.setText(getString(R.string.doctor_requests_subtitle_accepted_with_missed_fmt,
                            upcoming, acceptedMissed));
                } else {
                    subtitle.setText(getString(R.string.doctor_requests_subtitle_accepted_fmt, accepted));
                }
                break;
            default:
                subtitle.setText(getString(R.string.doctor_requests_subtitle_rejected_fmt, rejected));
                break;
        }
        if (tabLegendCard != null && tabLegendText != null) {
            if (selected == ReqStatus.PENDING
                    && !pendingItems.isEmpty()
                    && overdue > 0) {
                tabLegendText.setText(R.string.doctor_requests_pending_legend);
                tabLegendCard.setVisibility(View.VISIBLE);
            } else if (selected == ReqStatus.ACCEPTED
                    && !acceptedItems.isEmpty()
                    && acceptedMissed > 0) {
                tabLegendText.setText(R.string.doctor_requests_accepted_missed_legend);
                tabLegendCard.setVisibility(View.VISIBLE);
            } else {
                tabLegendCard.setVisibility(View.GONE);
            }
        }
        List<ReqListRow> displayRows = buildDisplayRows();
        adapter.submit(displayRows);
        if (reqEmpty != null && reqList != null) {
            boolean empty = displayRows.isEmpty();
            reqEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            reqList.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
        syncRequestsBadge();
    }

    private void syncRequestsBadge() {
        if (!(getActivity() instanceof DoctorMainActivity)) return;
        ((DoctorMainActivity) getActivity()).setDoctorRequestsPendingCount(actionablePendingCount());
    }

    private int countOverduePending() {
        int n = 0;
        for (DoctorReq r : pendingItems) {
            if (r.isPastSlot()) n++;
        }
        return n;
    }

    /** Pending có thể duyệt (chưa quá giờ bắt đầu slot). */
    private int actionablePendingCount() {
        int n = 0;
        for (DoctorReq r : pendingItems) {
            if (!r.isPastSlot()) n++;
        }
        return n;
    }

    private List<ReqListRow> buildDisplayRows() {
        switch (selected) {
            case PENDING:
                return buildPendingSectionedRows();
            case ACCEPTED:
                return buildAcceptedSectionedRows();
            default:
                return mapToItemRows(rejectedItems);
        }
    }

    private static List<ReqListRow> mapToItemRows(List<DoctorReq> from) {
        List<ReqListRow> out = new ArrayList<>();
        if (from == null) return out;
        for (DoctorReq r : from) {
            out.add(new ItemRow(r));
        }
        return out;
    }

    /**
     * Chờ duyệt: lịch còn trong khung giờ lên trước; lịch quá giờ gom xuống cuối (vẫn từ chối được).
     */
    private List<ReqListRow> buildPendingSectionedRows() {
        List<DoctorReq> actionable = new ArrayList<>();
        List<DoctorReq> past = new ArrayList<>();
        for (DoctorReq r : pendingItems) {
            if (r.isPastSlot()) {
                past.add(r);
            } else {
                actionable.add(r);
            }
        }
        List<ReqListRow> rows = new ArrayList<>();
        if (!actionable.isEmpty() && !past.isEmpty()) {
            rows.add(new SectionHeaderRow(
                    getString(R.string.doctor_requests_section_need_fmt, actionable.size()),
                    getString(R.string.doctor_requests_section_need_hint),
                    false));
        }
        for (DoctorReq r : actionable) {
            rows.add(new ItemRow(r));
        }
        if (!past.isEmpty()) {
            rows.add(new SectionHeaderRow(
                    getString(R.string.doctor_requests_section_overdue_title_fmt, past.size()),
                    getString(R.string.doctor_requests_section_overdue_hint),
                    true));
            for (DoctorReq r : past) {
                rows.add(new ItemRow(r));
            }
        }
        return rows;
    }

    private int countMissedAccepted() {
        int n = 0;
        for (DoctorReq r : acceptedItems) {
            if (r.isMissedAcceptedExam()) n++;
        }
        return n;
    }

    /**
     * Đã chấp nhận: lịch còn hạn trước; lịch quá giờ và chưa khám (theo trạng thái) xuống dưới, đánh dấu đỏ.
     */
    private List<ReqListRow> buildAcceptedSectionedRows() {
        List<DoctorReq> upcoming = new ArrayList<>();
        List<DoctorReq> missed = new ArrayList<>();
        for (DoctorReq r : acceptedItems) {
            if (r.isMissedAcceptedExam()) {
                missed.add(r);
            } else {
                upcoming.add(r);
            }
        }
        List<ReqListRow> rows = new ArrayList<>();
        if (!upcoming.isEmpty() && !missed.isEmpty()) {
            rows.add(new SectionHeaderRow(
                    getString(R.string.doctor_requests_section_accepted_upcoming_fmt, upcoming.size()),
                    getString(R.string.doctor_requests_section_accepted_upcoming_hint),
                    false));
        }
        for (DoctorReq r : upcoming) {
            rows.add(new ItemRow(r));
        }
        if (!missed.isEmpty()) {
            rows.add(new SectionHeaderRow(
                    getString(R.string.doctor_requests_section_accepted_missed_title_fmt, missed.size()),
                    getString(R.string.doctor_requests_section_accepted_missed_hint),
                    true));
            for (DoctorReq r : missed) {
                rows.add(new ItemRow(r));
            }
        }
        return rows;
    }

    private void loadAppointmentTabs() {
        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            Toast.makeText(requireContext(), R.string.doctor_panel_session_required, Toast.LENGTH_SHORT).show();
            pendingItems.clear();
            acceptedItems.clear();
            rejectedItems.clear();
            refresh();
            return;
        }
        String auth = "Bearer " + s.accessToken;
        repository.fetchAllTabs(auth, (tabs, err) -> {
            if (!isAdded()) return;
            if (err != null) {
                Toast.makeText(requireContext(), getString(R.string.doctor_panel_load_failed_fmt, err), Toast.LENGTH_LONG).show();
                pendingItems.clear();
                acceptedItems.clear();
                rejectedItems.clear();
                refresh();
                return;
            }
            pendingItems.clear();
            acceptedItems.clear();
            rejectedItems.clear();
            pendingItems.addAll(mapRows(tabs.pending, ReqStatus.PENDING));
            acceptedItems.addAll(mapRows(tabs.confirmed, ReqStatus.ACCEPTED));
            rejectedItems.addAll(mapRows(tabs.cancelled, ReqStatus.REJECTED));
            refresh();
            enrichMissingPatientAges(auth);
        });
    }

    /**
     * Gọi GET chi tiết lịch khi danh sách không có tuổi (để lấy {@code patientDob} giống web-admin).
     */
    private void enrichMissingPatientAges(String auth) {
        HashSet<Integer> seen = new HashSet<>();
        List<DoctorReq> targets = new ArrayList<>();
        collectAgeEnrichTargets(pendingItems, seen, targets);
        collectAgeEnrichTargets(acceptedItems, seen, targets);
        collectAgeEnrichTargets(rejectedItems, seen, targets);
        if (targets.isEmpty()) return;
        AtomicInteger remaining = new AtomicInteger(targets.size());
        for (DoctorReq r : targets) {
            repository.fetchAppointmentDetail(auth, r.appointmentId, (dto, err) -> {
                if (isAdded() && dto != null) {
                    applyDetail(r, dto);
                }
                if (remaining.decrementAndGet() == 0 && isAdded()) {
                    refresh();
                }
            });
        }
    }

    private static void collectAgeEnrichTargets(List<DoctorReq> from, HashSet<Integer> seen, List<DoctorReq> targets) {
        for (DoctorReq r : from) {
            if (r.appointmentId <= 0 || r.age > 0) continue;
            if (!seen.add(r.appointmentId)) continue;
            targets.add(r);
        }
    }

    private static void applyDetail(DoctorReq r, DoctorAppointmentDetailDto d) {
        int a = DoctorPanelFormatters.ageFromDateString(d.patientDob);
        if (a > 0) r.age = a;
        if (d.patientGender != null && !d.patientGender.trim().isEmpty()) {
            r.gender = DoctorPanelFormatters.displayGender(d.patientGender);
        }
    }

    private static List<DoctorReq> mapRows(List<DoctorAppointmentDto> dtos, ReqStatus tab) {
        List<DoctorReq> out = new ArrayList<>();
        if (dtos == null) return out;
        for (DoctorAppointmentDto d : dtos) {
            out.add(DoctorReq.from(d, tab));
        }
        return out;
    }

    private void confirmRequest(@NonNull DoctorReq r) {
        if (r.appointmentId <= 0) return;
        if (r.isPastSlot()) {
            Toast.makeText(requireContext(), R.string.doctor_request_cannot_accept_past, Toast.LENGTH_LONG).show();
            return;
        }
        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            Toast.makeText(requireContext(), R.string.doctor_panel_session_required, Toast.LENGTH_SHORT).show();
            return;
        }
        repository.confirmAppointment("Bearer " + s.accessToken, r.appointmentId, (ok, err) -> {
            if (!isAdded()) return;
            if (!ok) {
                Toast.makeText(requireContext(), getString(R.string.doctor_panel_action_failed_fmt, err != null ? err : ""), Toast.LENGTH_LONG).show();
                return;
            }
            loadAppointmentTabs();
        });
    }

    private void cancelRequest(@NonNull DoctorReq r) {
        if (r.appointmentId <= 0) return;
        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            Toast.makeText(requireContext(), R.string.doctor_panel_session_required, Toast.LENGTH_SHORT).show();
            return;
        }
        repository.cancelAppointment("Bearer " + s.accessToken, r.appointmentId, (ok, err) -> {
            if (!isAdded()) return;
            if (!ok) {
                Toast.makeText(requireContext(), getString(R.string.doctor_panel_action_failed_fmt, err != null ? err : ""), Toast.LENGTH_LONG).show();
                return;
            }
            loadAppointmentTabs();
        });
    }

    class ReqAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        static final int TYPE_HEADER = 0;
        static final int TYPE_ITEM = 1;

        final List<ReqListRow> rows = new ArrayList<>();

        void submit(List<ReqListRow> data) {
            rows.clear();
            rows.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            ReqListRow row = rows.get(position);
            if (row instanceof SectionHeaderRow) return TYPE_HEADER;
            return TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_doctor_request_section_header, parent, false);
                return new SectionH(v);
            }
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_doctor_request, parent, false);
            return new ItemH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ReqListRow row = rows.get(position);
            if (holder instanceof SectionH) {
                SectionHeaderRow sh = (SectionHeaderRow) row;
                SectionH h = (SectionH) holder;
                h.title.setText(sh.title);
                h.hint.setText(sh.subtitle);
                Context ctx = h.itemView.getContext();
                if (sh.overdueStyle) {
                    h.card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.doctor_request_section_overdue_bg));
                    h.card.setStrokeColor(ContextCompat.getColor(ctx, R.color.doctor_request_section_stroke_overdue));
                    h.accent.setBackgroundColor(ContextCompat.getColor(ctx, R.color.doctor_reject_red));
                } else {
                    h.card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.doctor_request_section_need_bg));
                    h.card.setStrokeColor(ContextCompat.getColor(ctx, R.color.doctor_request_section_stroke_need));
                    h.accent.setBackgroundColor(ContextCompat.getColor(ctx, R.color.doctor_brand_primary));
                }
            } else if (holder instanceof ItemH) {
                bindItem((ItemH) holder, ((ItemRow) row).req);
            }
        }

        private void bindItem(@NonNull ItemH h, @NonNull DoctorReq r) {
            DoctorPanelUi.stylePatientAvatar(h.avatarInitial, r.name, h.itemView.getContext());
            h.name.setText(r.name);
            if (r.age > 0) {
                h.ageGender.setText(getString(R.string.doctor_age_gender_fmt, r.age, r.gender));
            } else {
                h.ageGender.setText(r.gender);
            }
            h.date.setText(getString(R.string.doctor_request_date_fmt, r.date));
            h.time.setText(getString(R.string.doctor_request_time_fmt, r.time));
            h.reason.setText(getString(R.string.doctor_request_reason_fmt, r.reason));
            h.priorityRow.setVisibility(r.priority ? View.VISIBLE : View.GONE);

            boolean overduePending = r.tab == ReqStatus.PENDING && r.isPastSlot();
            boolean missedAccepted = r.isMissedAcceptedExam();
            if (overduePending) {
                h.overdueChip.setText(R.string.doctor_request_overdue_chip);
                h.overdueChip.setVisibility(View.VISIBLE);
            } else if (missedAccepted) {
                h.overdueChip.setText(R.string.doctor_request_missed_exam_chip);
                h.overdueChip.setVisibility(View.VISIBLE);
            } else {
                h.overdueChip.setVisibility(View.GONE);
            }

            if (r.tab == ReqStatus.PENDING) {
                h.actionRow.setVisibility(View.VISIBLE);
                h.btnAccept.setEnabled(!overduePending);
                h.btnAccept.setAlpha(overduePending ? 0.45f : 1f);
                h.btnAccept.setOnClickListener(v -> confirmRequest(r));
                h.btnReject.setOnClickListener(v -> cancelRequest(r));
            } else {
                h.actionRow.setVisibility(View.GONE);
            }

            float density = h.itemView.getResources().getDisplayMetrics().density;
            if (overduePending || missedAccepted) {
                h.root.setCardBackgroundColor(ContextCompat.getColor(h.itemView.getContext(),
                        R.color.doctor_request_card_overdue_bg));
                h.root.setStrokeWidth(Math.round(1 * density));
                h.root.setStrokeColor(ContextCompat.getColor(h.itemView.getContext(),
                        R.color.doctor_request_card_overdue_stroke));
            } else if (r.priority && r.tab == ReqStatus.PENDING) {
                h.root.setCardBackgroundColor(ContextCompat.getColor(h.itemView.getContext(), R.color.white));
                h.root.setStrokeWidth(Math.round(2 * density));
                h.root.setStrokeColor(ContextCompat.getColor(h.itemView.getContext(), R.color.doctor_priority_orange));
            } else {
                h.root.setCardBackgroundColor(ContextCompat.getColor(h.itemView.getContext(), R.color.white));
                h.root.setStrokeWidth(Math.round(1 * density));
                h.root.setStrokeColor(ContextCompat.getColor(h.itemView.getContext(), R.color.doctor_stat_card_stroke));
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class SectionH extends RecyclerView.ViewHolder {
            final com.google.android.material.card.MaterialCardView card;
            final View accent;
            final TextView title;
            final TextView hint;

            SectionH(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.doctor_req_section_card);
                accent = itemView.findViewById(R.id.doctor_req_section_accent);
                title = itemView.findViewById(R.id.doctor_req_section_title);
                hint = itemView.findViewById(R.id.doctor_req_section_hint);
            }
        }

        class ItemH extends RecyclerView.ViewHolder {
            final com.google.android.material.card.MaterialCardView root;
            final TextView overdueChip;
            final TextView avatarInitial;
            final TextView name, ageGender, date, time, reason;
            final MaterialButton btnAccept, btnReject;
            final View priorityRow, actionRow;

            ItemH(@NonNull View itemView) {
                super(itemView);
                root = (com.google.android.material.card.MaterialCardView) itemView;
                overdueChip = itemView.findViewById(R.id.req_overdue_chip);
                avatarInitial = itemView.findViewById(R.id.req_avatar_initial);
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
