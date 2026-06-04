package com.example.frontend_bookingcare.ui.account;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.account.AccountFlowListener;
import com.example.frontend_bookingcare.api.PatientAppointmentDto;
import com.example.frontend_bookingcare.data.AuthRepository;
import com.example.frontend_bookingcare.data.PatientAppointmentsRepository;
import com.example.frontend_bookingcare.data.PatientProfileRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.ProfileExtras;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.booking.BookingFormatters;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class ProfileFragment extends Fragment {

    private LinearLayout healthList;
    private View healthEmptyWrap;
    private TextView healthEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.profile_toolbar);
        AccountUiHelper.bindToolbarBack(this, toolbar);
        healthList = view.findViewById(R.id.profile_health_list);
        healthEmptyWrap = view.findViewById(R.id.profile_health_empty_wrap);
        healthEmpty = view.findViewById(R.id.profile_health_empty);
        setupInfoRows(view);
        bindProfile(view);
        syncProfileFromServer(view);
        loadHealthRecords();
        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        AccountFlowListener flow = parent;
        AuthRepository repo = parent.getAuthRepository();

        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> flow.openEditProfile());
        view.findViewById(R.id.btn_change_password).setOnClickListener(v -> flow.openChangePassword());
        view.findViewById(R.id.btn_logout).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.logout)
                        .setMessage(R.string.account_logout_confirm)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.logout, (d, w) ->
                                repo.logout((a, err) -> {
                                    Toast.makeText(requireContext(), R.string.account_logged_out_toast, Toast.LENGTH_SHORT).show();
                                    if (requireActivity() instanceof MainActivity) {
                                        ((MainActivity) requireActivity()).refreshAfterAuthChange();
                                    }
                                    parent.getChildFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                    parent.getChildFragmentManager().beginTransaction()
                                            .replace(R.id.account_inner_container, new AccountHubFragment())
                                            .commit();
                                }))
                        .show());
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) {
            bindProfile(v);
            syncProfileFromServer(v);
            loadHealthRecords();
        }
    }

    private void setupInfoRows(View root) {
        setupInfoRow(root.findViewById(R.id.profile_phone_row),
                R.drawable.bg_profile_icon_blue, R.drawable.ic_doctor_phone, R.color.brand_primary,
                R.string.profile_label_phone);
        setupInfoRow(root.findViewById(R.id.profile_dob_row),
                R.drawable.bg_profile_icon_teal, R.drawable.ic_profile_calendar, R.color.teal_primary,
                R.string.profile_label_dob);
        setupInfoRow(root.findViewById(R.id.profile_gender_row),
                R.drawable.bg_profile_icon_purple, R.drawable.ic_nav_account, R.color.home_tile_purple,
                R.string.profile_label_gender);
        setupInfoRow(root.findViewById(R.id.profile_address_row),
                R.drawable.bg_profile_icon_peach, R.drawable.ic_profile_location, R.color.account_fab_orange,
                R.string.profile_label_address);
    }

    private void setupInfoRow(@NonNull View row, int iconBgRes, int iconRes, int iconTintColorRes, int labelRes) {
        row.findViewById(R.id.profile_row_icon_bg).setBackgroundResource(iconBgRes);
        ImageView icon = row.findViewById(R.id.profile_row_icon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), iconTintColorRes));
        ((TextView) row.findViewById(R.id.profile_row_label)).setText(labelRes);
    }

    private void bindProfile(View view) {
        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        SessionManager sm = parent.getSessionManager();
        AuthSession s = sm.getSession();
        ProfileExtras ex = sm.getProfileExtras();

        ((TextView) view.findViewById(R.id.profile_name)).setText(s != null && s.fullName != null ? s.fullName : "—");
        ((TextView) view.findViewById(R.id.profile_email)).setText(s != null && s.email != null ? s.email : "—");

        setInfoValue(view.findViewById(R.id.profile_phone_row), nz(ex.phone));
        setInfoValue(view.findViewById(R.id.profile_dob_row), nz(ex.dob));
        setInfoValue(view.findViewById(R.id.profile_gender_row),
                nz(BookingFormatters.displayGender(requireContext(), ex.gender)));
        setInfoValue(view.findViewById(R.id.profile_address_row), nz(ex.address));
    }

    private static void setInfoValue(@NonNull View row, String value) {
        ((TextView) row.findViewById(R.id.profile_row_value)).setText(value);
    }

    private void syncProfileFromServer(View view) {
        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        SessionManager sm = parent.getSessionManager();
        AuthSession s = sm.getSession();
        if (s == null || s.accessToken == null || s.accessToken.isEmpty()) return;

        String bearer = "Bearer " + s.accessToken;
        new PatientProfileRepository().fetchMe(bearer, (extras, err) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (extras == null) {
                    if (err != null && !err.isEmpty()) {
                        Toast.makeText(requireContext(),
                                getString(R.string.profile_load_failed_fmt, err), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                sm.saveProfileExtras(extras);
                bindProfile(view);
            });
        });
    }

    private void loadHealthRecords() {
        if (healthList == null || healthEmptyWrap == null || !isAdded()) return;
        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        AuthSession s = parent.getSessionManager().getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            healthList.removeAllViews();
            healthEmptyWrap.setVisibility(View.VISIBLE);
            healthEmpty.setText(R.string.health_records_login_required);
            return;
        }
        new PatientAppointmentsRepository().fetch("Bearer " + s.accessToken, "COMPLETED", (list, err) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> renderHealthRecords(list, err));
        });
    }

    private void renderHealthRecords(@Nullable List<PatientAppointmentDto> list, @Nullable String err) {
        healthList.removeAllViews();
        if (err != null) {
            healthEmptyWrap.setVisibility(View.VISIBLE);
            healthEmpty.setText(err);
            return;
        }
        if (list == null || list.isEmpty()) {
            healthEmptyWrap.setVisibility(View.VISIBLE);
            healthEmpty.setText(R.string.health_records_empty);
            return;
        }
        healthEmptyWrap.setVisibility(View.GONE);
        LayoutInflater inf = LayoutInflater.from(requireContext());
        int limit = Math.min(list.size(), 10);
        for (int i = 0; i < limit; i++) {
            PatientAppointmentDto appt = list.get(i);
            View row = inf.inflate(R.layout.item_health_record, healthList, false);
            TextView title = row.findViewById(R.id.record_title);
            TextView date = row.findViewById(R.id.record_date);
            TextView doctor = row.findViewById(R.id.record_doctor);
            TextView note = row.findViewById(R.id.record_note);

            String specialty = !TextUtils.isEmpty(appt.specialty) ? appt.specialty : getString(R.string.health_records_visit);
            title.setText(specialty);
            date.setText(BookingFormatters.prettyDate(requireContext(), appt.appointmentDate));
            String doctorLabel = !TextUtils.isEmpty(appt.doctorName) ? appt.doctorName : "—";
            doctor.setText(getString(R.string.health_record_doctor_fmt, doctorLabel));

            String noteText = !TextUtils.isEmpty(appt.clinicalNote)
                    ? appt.clinicalNote
                    : (!TextUtils.isEmpty(appt.reason) ? appt.reason : getString(R.string.health_record_no_note));
            note.setText(noteText);
            healthList.addView(row);
        }
    }

    private static String nz(String s) {
        return s == null || s.isEmpty() ? "—" : s;
    }
}
