package com.example.frontend_bookingcare.ui.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.support.chat.CustomerCareChatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class SupportBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static SupportBottomSheetDialogFragment newInstance() {
        return new SupportBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_support, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton close = view.findViewById(R.id.support_close);
        close.setOnClickListener(v -> dismiss());

        view.findViewById(R.id.support_chat).setOnClickListener(v -> openSupportChat());

        MaterialButton callBtn = view.findViewById(R.id.support_call);
        String display = getString(R.string.account_support_phone_display);
        callBtn.setText(getString(R.string.account_support_call_fmt, display));

        view.findViewById(R.id.support_call).setOnClickListener(v -> {
            dismiss();
            Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse(getString(R.string.account_support_phone_uri)));
            startActivity(dial);
        });
    }

    private void openSupportChat() {
        SessionManager sm = new SessionManager(requireContext());
        if (!sm.isLoggedIn()) {
            dismiss();
            Toast.makeText(requireContext(), R.string.chat_login_required, Toast.LENGTH_SHORT).show();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(R.id.nav_account);
            }
            return;
        }
        dismiss();
        startActivity(CustomerCareChatActivity.newIntent(
                requireContext(),
                "support:cskh",
                getString(R.string.support_cskh_title),
                null,
                false));
    }
}
