package com.example.frontend_bookingcare.ui.account;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.data.AuthRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordNewPasswordFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password_new_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.forgot_new_pw_toolbar);
        AccountUiHelper.bindToolbarBack(this, toolbar);

        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        AuthRepository repo = parent.getAuthRepository();

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!AccountUiHelper.popAccountInnerBack(ForgotPasswordNewPasswordFragment.this)) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        TextInputEditText pw = view.findViewById(R.id.input_forgot_new_password);
        TextInputEditText confirm = view.findViewById(R.id.input_forgot_new_password_confirm);
        MaterialButton btn = view.findViewById(R.id.btn_forgot_reset_submit);

        btn.setOnClickListener(v -> {
            String p = text(pw);
            String c = text(confirm);
            if (TextUtils.isEmpty(p) || p.length() < 6) {
                Toast.makeText(requireContext(), R.string.auth_create_password_desc, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!p.equals(c)) {
                Toast.makeText(requireContext(), R.string.auth_password_mismatch, Toast.LENGTH_SHORT).show();
                return;
            }
            String email = parent.forgotPasswordDraft.email;
            String otp = parent.forgotPasswordDraft.otp;
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(otp)) {
                Toast.makeText(requireContext(), R.string.auth_login_missing, Toast.LENGTH_SHORT).show();
                return;
            }
            btn.setEnabled(false);
            repo.confirmPasswordReset(email, otp, p, (unused, err) -> {
                btn.setEnabled(true);
                if (err != null) {
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), R.string.auth_forgot_success, Toast.LENGTH_SHORT).show();
                    parent.forgotPasswordDraft.clear();
                    parent.navigateToLoginPrefilled(email);
                }
            });
        });
    }

    private static String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }
}
