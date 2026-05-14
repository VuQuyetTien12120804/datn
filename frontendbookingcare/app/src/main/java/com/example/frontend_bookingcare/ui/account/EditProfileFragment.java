package com.example.frontend_bookingcare.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.UpdatePatientProfileRequest;
import com.example.frontend_bookingcare.data.PatientProfileRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.ProfileExtras;
import com.example.frontend_bookingcare.session.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class EditProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.edit_profile_toolbar);
        AccountUiHelper.bindToolbarBack(this, toolbar);

        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        SessionManager sm = parent.getSessionManager();
        ProfileExtras ex = sm.getProfileExtras();

        TextInputEditText phone = view.findViewById(R.id.input_phone);
        TextInputEditText dob = view.findViewById(R.id.input_dob);
        TextInputEditText gender = view.findViewById(R.id.input_gender);
        TextInputEditText address = view.findViewById(R.id.input_address);
        phone.setText(ex.phone);
        dob.setText(ex.dob);
        gender.setText(ex.gender);
        address.setText(ex.address);

        MaterialButton save = view.findViewById(R.id.btn_save_profile);
        save.setOnClickListener(v -> {
            ProfileExtras next = new ProfileExtras(
                    text(phone),
                    text(dob),
                    text(gender),
                    text(address)
            );
            AuthSession s = sm.getSession();
            if (s != null && s.accessToken != null && !s.accessToken.isEmpty()) {
                // Logged-in: lưu cả DB để lần sau không mất.
                save.setEnabled(false);
                String bearer = "Bearer " + s.accessToken;
                new PatientProfileRepository().updateMe(
                        bearer,
                        new UpdatePatientProfileRequest(next.phone, next.dob, next.gender, next.address),
                        new PatientProfileRepository.RepoCallback() {
                            @Override
                            public void onSuccess(com.example.frontend_bookingcare.api.ApiEnvelope env) {
                                requireActivity().runOnUiThread(() -> {
                                    sm.saveProfileExtras(next);
                                    Toast.makeText(requireContext(), R.string.edit_profile_saved_toast, Toast.LENGTH_SHORT).show();
                                    parent.getChildFragmentManager().popBackStack();
                                });
                            }

                            @Override
                            public void onError(String message) {
                                requireActivity().runOnUiThread(() -> {
                                    save.setEnabled(true);
                                    Toast.makeText(requireContext(), "Lưu thất bại: " + message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                );
            } else {
                // Not logged in: fallback lưu local.
                sm.saveProfileExtras(next);
                Toast.makeText(requireContext(), R.string.edit_profile_saved_toast, Toast.LENGTH_SHORT).show();
                parent.getChildFragmentManager().popBackStack();
            }
        });
    }

    private static String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }
}
