package com.example.frontend_bookingcare.ui.account;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.data.AuthRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ChangePasswordFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.change_pwd_toolbar);
        AccountUiHelper.bindToolbarBack(this, toolbar);

        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        AuthRepository repo = parent.getAuthRepository();

        TextInputEditText oldPw = view.findViewById(R.id.input_old_password);
        TextInputEditText newPw = view.findViewById(R.id.input_new_password);
        TextInputEditText confirm = view.findViewById(R.id.input_confirm_password);
        MaterialButton save = view.findViewById(R.id.btn_save_password);

        save.setOnClickListener(v -> {
            String o = text(oldPw);
            String n = text(newPw);
            String c = text(confirm);
            if (TextUtils.isEmpty(o) || TextUtils.isEmpty(n)) {
                Toast.makeText(requireContext(), R.string.change_pwd_fill_all, Toast.LENGTH_SHORT).show();
                return;
            }
            if (n.length() < 6) {
                Toast.makeText(requireContext(), R.string.change_pwd_min_length, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!n.equals(c)) {
                Toast.makeText(requireContext(), R.string.change_pwd_mismatch, Toast.LENGTH_SHORT).show();
                return;
            }
            save.setEnabled(false);
            repo.changePassword(o, n, (unused, err) -> {
                save.setEnabled(true);
                if (err != null) {
                    Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), R.string.change_pwd_success, Toast.LENGTH_SHORT).show();
                    parent.getChildFragmentManager().popBackStack();
                }
            });
        });
    }

    private static String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString() : "";
    }
}
