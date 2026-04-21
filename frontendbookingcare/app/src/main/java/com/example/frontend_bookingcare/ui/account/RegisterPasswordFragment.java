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
import com.example.frontend_bookingcare.account.AccountFlowListener;
import com.example.frontend_bookingcare.account.AuthUiUtils;
import com.example.frontend_bookingcare.data.AuthRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterPasswordFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.register_password_toolbar);
        AccountUiHelper.bindToolbarBack(this, toolbar);

        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        AuthRepository repo = parent.getAuthRepository();
        AccountFlowListener flow = parent;

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!AccountUiHelper.popAccountInnerBack(RegisterPasswordFragment.this)) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        TextInputEditText fullName = view.findViewById(R.id.input_full_name);
        TextInputEditText password = view.findViewById(R.id.input_password);
        TextInputEditText confirm = view.findViewById(R.id.input_password_confirm);
        MaterialButton btn = view.findViewById(R.id.btn_register_password_continue);

        btn.setOnClickListener(v -> {
            String fn = text(fullName);
            String pw = text(password);
            String c = text(confirm);
            if (TextUtils.isEmpty(fn)) {
                Toast.makeText(requireContext(), R.string.full_name, Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(pw) || pw.length() < 6) {
                Toast.makeText(requireContext(), R.string.auth_create_password_desc, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!pw.equals(c)) {
                Toast.makeText(requireContext(), R.string.auth_password_mismatch, Toast.LENGTH_SHORT).show();
                return;
            }
            parent.registerDraft.fullName = fn;
            parent.registerDraft.password = pw;
            btn.setEnabled(false);
            repo.register(parent.registerDraft, (session, err) -> {
                btn.setEnabled(true);
                if (err != null) {
                    if (AuthUiUtils.isEmailAlreadyRegisteredMessage(err)) {
                        String emailUsed = parent.registerDraft.email;
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.auth_email_exists_title)
                                .setMessage(R.string.auth_email_exists_message)
                                .setNegativeButton(R.string.auth_email_exists_other_email, (d, w) ->
                                        parent.navigateToRegisterFresh())
                                .setPositiveButton(R.string.auth_email_exists_login, (d, w) ->
                                        parent.navigateToLoginPrefilled(emailUsed))
                                .show();
                    } else {
                        Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.auth_register_success, Toast.LENGTH_SHORT).show();
                    flow.onRegisterComplete();
                }
            });
        });
    }

    private static String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }
}
