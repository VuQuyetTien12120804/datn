package com.example.frontend_bookingcare.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.account.AccountFlowListener;
import com.example.frontend_bookingcare.data.AuthRepository;
import com.example.frontend_bookingcare.data.PatientProfileRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.ProfileExtras;
import com.example.frontend_bookingcare.session.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ProfileFragment extends Fragment {

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
        bindProfile(view);
        // Sync ngay khi mở màn (đừng chờ onResume), để chắc chắn lấy data từ DB.
        syncProfileFromServer(view);
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
        ((TextView) view.findViewById(R.id.profile_gender_row)).setText(getString(R.string.profile_row_gender, nz(ex.gender)));
        ((TextView) view.findViewById(R.id.profile_address_row)).setText(getString(R.string.profile_row_address, nz(ex.address)));

        LinearLayout health = view.findViewById(R.id.profile_health_list);
        health.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(requireContext());
        addHealthCard(inf, health,
                getString(R.string.demo_health_1_title),
                getString(R.string.demo_health_1_date),
                getString(R.string.demo_health_1_doctor),
                getString(R.string.demo_health_1_note));
        addHealthCard(inf, health,
                getString(R.string.demo_health_2_title),
                getString(R.string.demo_health_2_date),
                getString(R.string.demo_health_2_doctor),
                getString(R.string.demo_health_2_note));
    }

    /**
     * ProfileFragment trước đây chỉ đọc ProfileExtras local nên nếu app bị clear data /
     * logout-login / đổi máy thì UI vẫn là "—" dù DB đã có.
     * Giờ sẽ sync từ API /api/v1/patient/profile khi đang đăng nhập.
     */
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
                        Toast.makeText(requireContext(), "Không tải được hồ sơ: " + err, Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                sm.saveProfileExtras(extras);
                bindProfile(view);
            });
        });
    }

    private static void addHealthCard(LayoutInflater inf, LinearLayout parent, String title, String date, String doctor, String note) {
        View row = inf.inflate(R.layout.item_health_record, parent, false);
        ((TextView) row.findViewById(R.id.record_title)).setText(title);
        ((TextView) row.findViewById(R.id.record_date)).setText(date);
        ((TextView) row.findViewById(R.id.record_doctor)).setText(doctor);
        ((TextView) row.findViewById(R.id.record_note)).setText(note);
        parent.addView(row);
    }

    private static String nz(String s) {
        return s == null || s.isEmpty() ? "—" : s;
    }
}
