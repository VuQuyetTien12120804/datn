package com.example.frontend_bookingcare.ui.account;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.google.android.material.textfield.TextInputEditText;

public class VerifyOtpFragment extends Fragment {

    private static final long RESEND_INTERVAL_MS = 60_000L;

    private CountDownTimer resendTimer;
    private TextView resendView;
    private MaterialButton confirmBtn;
    private AuthRepository repo;
    private AccountFragment parent;
    private AccountFlowListener flow;
    private RegisterPasswordOpening flowPassword;

    /** Parent must route to password step after OTP verified. */
    public interface RegisterPasswordOpening {
        void openRegisterPassword();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_verify_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.verify_toolbar);
        AccountUiHelper.bindToolbarBack(this, toolbar);

        parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        repo = parent.getAuthRepository();
        flow = parent;
        flowPassword = parent;

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!AccountUiHelper.popAccountInnerBack(VerifyOtpFragment.this)) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        TextView hint = view.findViewById(R.id.verify_email_hint);
        hint.setText(getString(R.string.auth_otp_subtitle_fmt, parent.registerDraft.email));

        TextInputEditText otp = view.findViewById(R.id.input_otp);
        confirmBtn = view.findViewById(R.id.btn_verify_confirm);
        resendView = view.findViewById(R.id.verify_resend);
        TextView changeEmail = view.findViewById(R.id.verify_change_email);

        startResendCooldown();

        resendView.setOnClickListener(v -> {
            if (!resendView.isEnabled()) return;
            resendView.setEnabled(false);
            repo.requestRegisterOtp(parent.registerDraft.email, (otpDto, err) -> {
                if (err != null) {
                    resendView.setEnabled(true);
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                } else {
                    if (BuildConfig.DEBUG && otpDto != null && otpDto.otp != null && !otpDto.otp.isEmpty()) {
                        Toast.makeText(requireContext(), getString(R.string.auth_otp_dev_hint, otpDto.otp), Toast.LENGTH_LONG).show();
                    }
                    Toast.makeText(requireContext(), R.string.auth_otp_sent, Toast.LENGTH_SHORT).show();
                    startResendCooldown();
                }
            });
        });

        changeEmail.setOnClickListener(v -> AccountUiHelper.popAccountInnerBack(VerifyOtpFragment.this));

        confirmBtn.setOnClickListener(v -> {
            String code = otp.getText() != null ? otp.getText().toString().trim() : "";
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(requireContext(), R.string.auth_otp_required, Toast.LENGTH_SHORT).show();
                return;
            }
            confirmBtn.setEnabled(false);
            repo.verifyRegisterOtp(parent.registerDraft.email, code, (unused, err) -> {
                confirmBtn.setEnabled(true);
                if (err != null) {
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                } else {
                    flowPassword.openRegisterPassword();
                }
            });
        });
    }

    private void startResendCooldown() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        resendView.setEnabled(false);
        resendTimer = new CountDownTimer(RESEND_INTERVAL_MS, 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int sec = (int) (millisUntilFinished / 1000L);
                resendView.setText(getString(R.string.auth_otp_resend_fmt, sec));
            }

            @Override
            public void onFinish() {
                resendView.setText(R.string.auth_otp_resend_ready);
                resendView.setEnabled(true);
            }
        };
        resendTimer.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
    }
}
