package com.example.frontend_bookingcare.ui.doctor_panel;

import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class DoctorUiHelper {

    private DoctorUiHelper() {
    }

    static void applyTopInsetPadding(@NonNull View view, int extraDp) {
        final int startPad = view.getPaddingStart();
        final int topPad = view.getPaddingTop();
        final int endPad = view.getPaddingEnd();
        final int bottomPad = view.getPaddingBottom();
        final int extra = dp(view, extraDp);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPaddingRelative(startPad, topPad + topInset + extra, endPad, bottomPad);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    private static int dp(@NonNull View view, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, view.getResources().getDisplayMetrics()));
    }
}
