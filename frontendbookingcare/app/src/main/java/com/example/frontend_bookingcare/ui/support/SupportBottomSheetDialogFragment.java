package com.example.frontend_bookingcare.ui.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.ui.support.chat.CustomerCareChatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

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

        view.findViewById(R.id.support_chat).setOnClickListener(v -> {
            dismiss();
            startActivity(CustomerCareChatActivity.newIntent(requireContext(), "support:cskh", "Chăm Sóc Khách Hàng", null, false));
        });

        view.findViewById(R.id.support_call).setOnClickListener(v -> {
            dismiss();
            Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:19002805"));
            startActivity(dial);
        });
    }
}

