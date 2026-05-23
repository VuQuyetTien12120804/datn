package com.example.frontend_bookingcare.ui.account;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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
import com.example.frontend_bookingcare.ui.legal.LegalDocumentActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterEmailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register_email, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.register_email_toolbar);
        AccountUiHelper.bindToolbarBack(this, toolbar);

        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        AuthRepository repo = parent.getAuthRepository();
        AccountFlowListener flow = parent;

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!AccountUiHelper.popAccountInnerBack(RegisterEmailFragment.this)) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        TextInputEditText email = view.findViewById(R.id.input_register_email);
        MaterialButton btn = view.findViewById(R.id.btn_register_email_continue);

        TextView terms = view.findViewById(R.id.register_terms_text);
        bindTermsText(terms);

        btn.setOnClickListener(v -> {
            String em = email.getText() != null ? email.getText().toString().trim() : "";
            if (TextUtils.isEmpty(em)) {
                Toast.makeText(requireContext(), R.string.auth_email_required, Toast.LENGTH_SHORT).show();
                return;
            }
            parent.registerDraft.email = em;
            btn.setEnabled(false);
            repo.requestRegisterOtp(em, (otpDto, err) -> {
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
                    flow.openVerifyOtp();
                }
            });
        });
    }

    private void bindTermsText(TextView terms) {
        String prefix = getString(R.string.auth_terms_prefix);
        String link = getString(R.string.auth_terms_link);
        String suffix = getString(R.string.auth_terms_suffix);
        String full = prefix + link + suffix;
        SpannableString ss = new SpannableString(full);
        int start = prefix.length();
        int end = start + link.length();
        ss.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                LegalDocumentActivity.start(RegisterEmailFragment.this, LegalDocumentActivity.CODE_TERMS);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(requireContext().getColor(R.color.auth_link));
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        terms.setText(ss);
        terms.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
