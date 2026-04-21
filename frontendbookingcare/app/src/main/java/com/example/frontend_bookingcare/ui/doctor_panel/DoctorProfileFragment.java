package com.example.frontend_bookingcare.ui.doctor_panel;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.data.AuthRepository;
import com.example.frontend_bookingcare.session.SessionManager;

import java.util.Arrays;
import java.util.List;

public class DoctorProfileFragment extends Fragment {

    static class LeaderItem {
        final int rank;
        final String name;
        final String specialty;
        final int patients;
        final double rating;
        final boolean isYou;

        LeaderItem(int rank, String name, String specialty, int patients, double rating, boolean isYou) {
            this.rank = rank;
            this.name = name;
            this.specialty = specialty;
            this.patients = patients;
            this.rating = rating;
            this.isYou = isYou;
        }
    }

    private final String[] periods = new String[]{"Ngày", "Tuần", "Tháng", "Quý", "Năm"};
    private int selectedPeriod = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyTopInsetToHeader(view);
        bindHeader(view);
        bindStats(view);
        bindExpertise(view);
        bindContact(view);
        bindPeriodTabs(view);
        bindLeaderboard(view);
        bindLogout(view);
    }

    private void applyTopInsetToHeader(@NonNull View root) {
        View header = root.findViewById(R.id.doctor_profile_header);
        final int basePad = header.getPaddingTop();
        final int startPad = header.getPaddingStart();
        final int endPad = header.getPaddingEnd();
        final int bottomPad = header.getPaddingBottom();
        final int extra = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPaddingRelative(startPad, basePad + topInset + extra, endPad, bottomPad);
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    private void bindHeader(@NonNull View view) {
        SessionManager sm = new SessionManager(requireContext());
        String displayName = sm.getSession() != null && sm.getSession().fullName != null && !sm.getSession().fullName.isEmpty()
                ? sm.getSession().fullName : "BS. Nguyễn Văn Minh";
        ((TextView) view.findViewById(R.id.doctor_profile_name)).setText(displayName);
        ((TextView) view.findViewById(R.id.doctor_profile_title)).setText("Bác sĩ chuyên khoa II");
        ((TextView) view.findViewById(R.id.doctor_profile_specialty_chip)).setText("Nội khoa");
    }

    private void bindStats(@NonNull View view) {
        setupStat(view.findViewById(R.id.stat_total),   "👥", R.string.doctor_profile_stat_total,   "1247", R.color.doctor_stat_blue_bg);
        setupStat(view.findViewById(R.id.stat_today),   "✅", R.string.doctor_profile_stat_today,   "2/6",  R.color.doctor_stat_green_bg);
        setupStat(view.findViewById(R.id.stat_pending), "⏱", R.string.doctor_profile_stat_pending, "4",    R.color.doctor_stat_orange_bg);
        setupStat(view.findViewById(R.id.stat_rating),  "⭐", R.string.doctor_profile_stat_rating,  "4.8",  R.color.doctor_stat_amber_bg);
    }

    private void setupStat(View stat, String icon, int labelRes, String value, int bgColor) {
        ((TextView) stat.findViewById(R.id.stat_icon)).setText(icon);
        ((TextView) stat.findViewById(R.id.stat_label)).setText(labelRes);
        ((TextView) stat.findViewById(R.id.stat_value)).setText(value);
        com.google.android.material.card.MaterialCardView card =
                (com.google.android.material.card.MaterialCardView) stat;
        card.setCardBackgroundColor(getResources().getColor(bgColor, null));
    }

    private void bindExpertise(@NonNull View view) {
        setupField(view.findViewById(R.id.row_specialty), R.string.doctor_profile_field_specialty, "Nội khoa");
        setupField(view.findViewById(R.id.row_degree),    R.string.doctor_profile_field_degree,    "Bác sĩ chuyên khoa II");
        setupField(view.findViewById(R.id.row_license),   R.string.doctor_profile_field_license,   "BS-123456");
    }

    private void setupField(View row, int labelRes, String value) {
        ((TextView) row.findViewById(R.id.field_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.field_value)).setText(value);
    }

    private void bindContact(@NonNull View view) {
        SessionManager sm = new SessionManager(requireContext());
        String email = sm.getSession() != null && sm.getSession().email != null && !sm.getSession().email.isEmpty()
                ? sm.getSession().email : "bs.nvminh@hospital.vn";
        ((TextView) view.findViewById(R.id.doctor_profile_email)).setText(email);
        ((TextView) view.findViewById(R.id.doctor_profile_phone)).setText("0901 234 567");
        ((TextView) view.findViewById(R.id.doctor_profile_rank_chip))
                .setText(getString(R.string.doctor_leaderboard_rank_fmt, 3, 45));
    }

    private void bindPeriodTabs(@NonNull View view) {
        LinearLayout tabs = view.findViewById(R.id.doctor_profile_period_tabs);
        tabs.removeAllViews();
        int padH = dp(14);
        int padV = dp(8);
        for (int i = 0; i < periods.length; i++) {
            final int idx = i;
            TextView t = new TextView(requireContext());
            t.setText(periods[i]);
            t.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_body));
            t.setPadding(padH, padV, padH, padV);
            t.setGravity(android.view.Gravity.CENTER);
            t.setBackgroundResource(idx == selectedPeriod ? R.drawable.bg_doctor_tab_active : android.R.color.transparent);
            t.setTextColor(getResources().getColor(
                    idx == selectedPeriod ? R.color.doctor_current_card_stroke : R.color.text_primary, null));
            t.setOnClickListener(v -> { selectedPeriod = idx; bindPeriodTabs(view); });
            tabs.addView(t);
        }
    }

    private void bindLeaderboard(@NonNull View view) {
        LinearLayout list = view.findViewById(R.id.doctor_profile_leaderboard_list);
        list.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(requireContext());
        int[] palette = new int[]{R.drawable.bg_tile_amber, R.drawable.bg_tile_teal, R.drawable.bg_tile_orange,
                R.drawable.bg_tile_blue, R.drawable.bg_tile_pink};
        List<LeaderItem> data = Arrays.asList(
                new LeaderItem(1, "BS. Trần Văn An",      "Tim mạch", 98, 5.0, false),
                new LeaderItem(2, "BS. Lê Thị Bích",      "Nội khoa", 95, 4.9, false),
                new LeaderItem(3, "BS. Nguyễn Văn Minh",  "Nội khoa", 92, 4.8, true),
                new LeaderItem(4, "BS. Phạm Quốc Dũng",   "Ngoại khoa", 89, 4.7, false),
                new LeaderItem(5, "BS. Hoàng Thu Hà",     "Sản khoa", 87, 4.7, false)
        );
        for (LeaderItem item : data) {
            View row = inf.inflate(R.layout.item_doctor_leaderboard, list, false);
            ((TextView) row.findViewById(R.id.lb_rank)).setText(String.valueOf(item.rank));
            ((FrameLayout) row.findViewById(R.id.lb_rank_bg))
                    .setBackgroundResource(palette[(item.rank - 1) % palette.length]);
            ((TextView) row.findViewById(R.id.lb_name)).setText(item.name);
            ((TextView) row.findViewById(R.id.lb_specialty)).setText(item.specialty);
            ((TextView) row.findViewById(R.id.lb_patients))
                    .setText(getString(R.string.doctor_leaderboard_patients_fmt, item.patients));
            ((TextView) row.findViewById(R.id.lb_rating)).setText(String.valueOf(item.rating));
            row.findViewById(R.id.lb_you_chip).setVisibility(item.isYou ? View.VISIBLE : View.GONE);
            if (item.isYou) {
                row.setBackgroundResource(R.drawable.bg_doctor_tab_active);
            }
            list.addView(row);
        }
    }

    private void bindLogout(@NonNull View view) {
        view.findViewById(R.id.doctor_profile_logout).setOnClickListener(v -> {
            SessionManager sm = new SessionManager(requireContext());
            new AuthRepository(sm).logout((unused, err) -> {
                Toast.makeText(requireContext(), R.string.account_logged_out_toast, Toast.LENGTH_SHORT).show();
                Intent i = new Intent(requireContext(), MainActivity.class);
                // CLEAR_TASK + NEW_TASK: xoá toàn bộ back stack cũ của DoctorMainActivity
                // để bác sĩ vừa logout không thể "back" quay lại panel.
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                if (getActivity() != null) {
                    getActivity().finishAffinity();
                }
            });
        });
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
