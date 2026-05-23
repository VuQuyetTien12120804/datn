package com.example.frontend_bookingcare.ui.account;

import android.graphics.Color;
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

import com.example.frontend_bookingcare.BuildConfig;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.account.AccountFlowListener;
import com.example.frontend_bookingcare.data.AuthRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordEmailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password_email, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.forgot_email_toolbar);
        AccountUiHelper.bindToolbarBack(this, toolbar);

        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        AuthRepository repo = parent.getAuthRepository();
        AccountFlowListener flow = parent;

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!AccountUiHelper.popAccountInnerBack(ForgotPasswordEmailFragment.this)) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        TextInputEditText emailInput = view.findViewById(R.id.input_forgot_email);
        if (!TextUtils.isEmpty(parent.forgotPasswordDraft.email)) {
            emailInput.setText(parent.forgotPasswordDraft.email);
        }

        MaterialButton btn = view.findViewById(R.id.btn_forgot_email_continue);
        btn.setOnClickListener(v -> {
            String em = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            if (TextUtils.isEmpty(em)) {
                Toast.makeText(requireContext(), R.string.auth_email_required, Toast.LENGTH_SHORT).show();
                return;
            }
            parent.forgotPasswordDraft.email = em;
            btn.setEnabled(false);
            repo.requestPasswordResetOtp(em, (otpDto, err) -> {
                btn.setEnabled(true);
                if (err != null) {
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                } else {
                    if (BuildConfig.DEBUG && otpDto != null && otpDto.otp != null && !otpDto.otp.isEmpty()) {
                        Toast.makeText(requireContext(), getString(R.string.auth_otp_dev_hint, otpDto.otp), Toast.LENGTH_LONG).show();
                    }
                    Snackbar sb = Snackbar.make(view, R.string.auth_otp_sent, Snackbar.LENGTH_SHORT);
                    sb.setBackgroundTint(Color.parseColor("#2E7D32"));
                    sb.setTextColor(Color.WHITE);
                    sb.show();
                    flow.openForgotPasswordOtp();
                }
            });
        });
    }
}
