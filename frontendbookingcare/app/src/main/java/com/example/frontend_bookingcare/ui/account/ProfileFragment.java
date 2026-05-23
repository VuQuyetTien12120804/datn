package com.example.frontend_bookingcare.ui.account;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
        healthEmpty = view.findViewById(R.id.profile_health_empty);
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

    private void bindProfile(View view) {
        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        SessionManager sm = parent.getSessionManager();
        AuthSession s = sm.getSession();
        ProfileExtras ex = sm.getProfileExtras();

        ((TextView) view.findViewById(R.id.profile_name)).setText(s != null && s.fullName != null ? s.fullName : "—");
        ((TextView) view.findViewById(R.id.profile_email)).setText(s != null && s.email != null ? s.email : "—");

        ((TextView) view.findViewById(R.id.profile_phone_row)).setText(getString(R.string.profile_row_phone, nz(ex.phone)));
        ((TextView) view.findViewById(R.id.profile_dob_row)).setText(getString(R.string.profile_row_dob, nz(ex.dob)));
        ((TextView) view.findViewById(R.id.profile_gender_row)).setText(
                getString(R.string.profile_row_gender, nz(BookingFormatters.displayGender(requireContext(), ex.gender))));
        ((TextView) view.findViewById(R.id.profile_address_row)).setText(getString(R.string.profile_row_address, nz(ex.address)));
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
        if (healthList == null || healthEmpty == null || !isAdded()) return;
        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        AuthSession s = parent.getSessionManager().getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            healthList.removeAllViews();
            healthEmpty.setVisibility(View.VISIBLE);
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
            healthEmpty.setVisibility(View.VISIBLE);
            healthEmpty.setText(err);
            return;
        }
        if (list == null || list.isEmpty()) {
            healthEmpty.setVisibility(View.VISIBLE);
            healthEmpty.setText(R.string.health_records_empty);
            return;
        }
        healthEmpty.setVisibility(View.GONE);
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
