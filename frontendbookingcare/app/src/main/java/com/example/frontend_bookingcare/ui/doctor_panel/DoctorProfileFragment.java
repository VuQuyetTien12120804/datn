package com.example.frontend_bookingcare.ui.doctor_panel;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.frontend_bookingcare.data.DoctorPanelRepository;
import com.example.frontend_bookingcare.locale.LocaleStore;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class DoctorProfileFragment extends Fragment {

    private final DoctorPanelRepository doctorPanelRepo = new DoctorPanelRepository();
    @Nullable private TextView pendingStatValue;
    @Nullable private TextView todayStatValue;

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
        bindWorkplace(view);
        bindContact(view);
        bindLanguage(view);
        bindLogout(view);
        refreshLiveProfileStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) {
            refreshLanguageValue(v);
        }
        refreshLiveProfileStats();
    }

    private void refreshLiveProfileStats() {
        refreshPendingStatCount();
        refreshTodayStatCount();
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
        String raw = sm.getSession() != null && sm.getSession().fullName != null && !sm.getSession().fullName.isEmpty()
                ? sm.getSession().fullName.trim()
                : getString(R.string.doctor_profile_name_fallback);
        String withoutTitle = raw.replaceFirst("(?i)^(BS\\.|Bs\\.|Dr\\.)\\s*", "").trim();
        if (withoutTitle.isEmpty()) {
            withoutTitle = raw;
        }
        String displayName = (LocaleStore.isEnglish(requireContext())
                ? getString(R.string.doctor_title_prefix_en) : getString(R.string.doctor_title_prefix_vi))
                + withoutTitle;
        ((TextView) view.findViewById(R.id.doctor_profile_name)).setText(displayName);
        ((TextView) view.findViewById(R.id.doctor_profile_title)).setText(getString(R.string.doctor_profile_degree_demo));
        ((TextView) view.findViewById(R.id.doctor_profile_specialty_chip)).setText(getString(R.string.doctor_profile_specialty_value_demo));
        setAvatarInitial(view, withoutTitle);
    }

    private void setAvatarInitial(@NonNull View view, String nameWithoutTitle) {
        TextView tv = view.findViewById(R.id.doctor_profile_avatar_letter);
        String s = nameWithoutTitle != null ? nameWithoutTitle.trim() : "";
        if (s.isEmpty()) {
            s = "?";
        }
        tv.setText(s.substring(0, 1).toUpperCase(Locale.getDefault()));
    }

    private void bindStats(@NonNull View view) {
        View todayCard = view.findViewById(R.id.stat_today);
        todayStatValue = todayCard.findViewById(R.id.stat_value);
        setupStat(todayCard, "✅", R.string.doctor_profile_stat_today,
                "…", R.color.doctor_stat_green_bg);
        View pendingCard = view.findViewById(R.id.stat_pending);
        pendingStatValue = pendingCard.findViewById(R.id.stat_value);
        setupStat(pendingCard, "⏱", R.string.doctor_profile_stat_pending,
                "…", R.color.doctor_stat_orange_bg);
    }

    private void refreshPendingStatCount() {
        if (pendingStatValue == null) return;
        SessionManager sm = new SessionManager(requireContext());
        AuthSession session = sm.getSession();
        if (session == null || TextUtils.isEmpty(session.accessToken)) {
            pendingStatValue.setText("0");
            return;
        }
        doctorPanelRepo.fetchAllTabs("Bearer " + session.accessToken, (tabs, err) -> {
            if (!isAdded() || pendingStatValue == null) return;
            if (err != null || tabs == null) {
                pendingStatValue.setText("0");
                return;
            }
            int n = DoctorPanelFormatters.countActionablePending(tabs.pending);
            pendingStatValue.setText(String.valueOf(n));
        });
    }

    private void refreshTodayStatCount() {
        if (todayStatValue == null) return;
        SessionManager sm = new SessionManager(requireContext());
        AuthSession session = sm.getSession();
        if (session == null || TextUtils.isEmpty(session.accessToken)) {
            todayStatValue.setText("0/0");
            return;
        }
        doctorPanelRepo.fetchToday("Bearer " + session.accessToken, (list, err) -> {
            if (!isAdded() || todayStatValue == null) return;
            if (err != null) {
                todayStatValue.setText("0/0");
                return;
            }
            todayStatValue.setText(DoctorPanelFormatters.formatTodayCompletedRatio(list));
        });
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
        setupField(view.findViewById(R.id.row_specialty), R.string.doctor_profile_field_specialty,
                getString(R.string.doctor_profile_specialty_value_demo));
        setupField(view.findViewById(R.id.row_degree), R.string.doctor_profile_field_degree,
                getString(R.string.doctor_profile_degree_demo));
        setupField(view.findViewById(R.id.row_license), R.string.doctor_profile_field_license,
                getString(R.string.doctor_profile_license_demo));
    }

    private void bindWorkplace(@NonNull View view) {
        setupField(view.findViewById(R.id.row_room), R.string.doctor_profile_field_room,
                getString(R.string.doctor_profile_room_demo));
        setupField(view.findViewById(R.id.row_unit), R.string.doctor_profile_field_unit,
                getString(R.string.doctor_profile_unit_demo));
        setupField(view.findViewById(R.id.row_schedule), R.string.doctor_profile_field_schedule,
                getString(R.string.doctor_profile_schedule_demo));
    }

    private void setupField(View row, int labelRes, String value) {
        ((TextView) row.findViewById(R.id.field_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.field_value)).setText(value);
    }

    private void bindContact(@NonNull View view) {
        SessionManager sm = new SessionManager(requireContext());
        String email = "bs.nvminh@hospital.vn";
        if (sm.getSession() != null && sm.getSession().email != null && !sm.getSession().email.isEmpty()) {
            email = sm.getSession().email;
        }
        ((TextView) view.findViewById(R.id.doctor_profile_email)).setText(email);
        ((TextView) view.findViewById(R.id.doctor_profile_phone)).setText(getString(R.string.doctor_profile_phone_demo));
    }

    private void bindLanguage(@NonNull View view) {
        View row = view.findViewById(R.id.doctor_language_row);
        row.setOnClickListener(v -> showLanguagePicker());
        refreshLanguageValue(view);
    }

    private void refreshLanguageValue(@NonNull View view) {
        TextView value = view.findViewById(R.id.doctor_profile_language_value);
        if (value == null) return;
        value.setText(LocaleStore.isEnglish(requireContext())
                ? getString(R.string.language_name_en)
                : getString(R.string.language_name_vi));
    }

    private void showLanguagePicker() {
        String[] options = new String[]{
                getString(R.string.language_name_vi),
                getString(R.string.language_name_en)
        };
        int checked = LocaleStore.isEnglish(requireContext()) ? 1 : 0;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_language_dialog_title)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    String tag = which == 1 ? "en" : "vi";
                    LocaleStore.saveAndApply(requireContext(), tag);
                    dialog.dismiss();
                    Toast.makeText(requireContext(), R.string.language_changed, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void bindLogout(@NonNull View view) {
        view.findViewById(R.id.doctor_profile_logout).setOnClickListener(v -> {
            SessionManager sm = new SessionManager(requireContext());
            new AuthRepository(sm).logout((unused, err) -> {
                Toast.makeText(requireContext(), R.string.account_logged_out_toast, Toast.LENGTH_SHORT).show();
                Intent i = new Intent(requireContext(), MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                if (getActivity() != null) {
                    getActivity().finishAffinity();
                }
            });
        });
    }
}
