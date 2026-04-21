package com.example.frontend_bookingcare.ui.account;

import android.os.Bundle;
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

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.account.AccountFlowListener;
import com.example.frontend_bookingcare.data.AuthRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginFragment extends Fragment {

    private static final String ARG_PREFILL_EMAIL = "prefill_email";

    public static LoginFragment newInstance(@Nullable String prefillEmail) {
        LoginFragment f = new LoginFragment();
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            Bundle args = new Bundle();
            args.putString(ARG_PREFILL_EMAIL, prefillEmail);
            f.setArguments(args);
        }
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.login_toolbar);
        AccountUiHelper.bindToolbarBack(this, toolbar);

        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        AuthRepository repo = parent.getAuthRepository();
        AccountFlowListener flow = parent;

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!AccountUiHelper.popAccountInnerBack(LoginFragment.this)) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        TextInputEditText email = view.findViewById(R.id.input_email);
        TextInputEditText password = view.findViewById(R.id.input_password);
        MaterialButton submit = view.findViewById(R.id.btn_submit_login);
        MaterialButton goRegister = view.findViewById(R.id.btn_go_register);
        TextView forgot = view.findViewById(R.id.login_forgot_password);

        Bundle args = getArguments();
        if (args != null) {
            String pre = args.getString(ARG_PREFILL_EMAIL);
            if (pre != null && !pre.isEmpty()) {
                email.setText(pre);
                password.requestFocus();
            }
        }

        forgot.setOnClickListener(v -> {
            String em = email.getText() != null ? email.getText().toString().trim() : "";
            parent.forgotPasswordDraft.clear();
            parent.forgotPasswordDraft.email = em;
            flow.openForgotPassword();
        });

        submit.setOnClickListener(v -> {
            String em = email.getText() != null ? email.getText().toString().trim() : "";
            String pw = password.getText() != null ? password.getText().toString() : "";
            if (TextUtils.isEmpty(em) || TextUtils.isEmpty(pw)) {
                Toast.makeText(requireContext(), R.string.auth_login_missing, Toast.LENGTH_SHORT).show();
                return;
            }
            submit.setEnabled(false);
            repo.login(em, pw, (session, err) -> {
                submit.setEnabled(true);
                if (err != null) {
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                } else {
                    flow.onLoggedIn();
                }
            });
        });

        goRegister.setOnClickListener(v -> flow.openRegister());
    }
}
