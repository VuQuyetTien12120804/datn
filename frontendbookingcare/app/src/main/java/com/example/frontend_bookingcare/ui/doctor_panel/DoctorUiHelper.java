package com.example.frontend_bookingcare.ui.doctor_panel;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class DoctorUiHelper {

    private DoctorUiHelper() {
    }

    /** Padding trên toàn màn (Schedule / Examining). */
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

    /**
     * Inset + khoảng cách trên hero card (Requests / Messages).
     * Bỏ qua nếu view null (include có thể ghi đè id gốc).
     */
    static void applyHeroTopInset(@Nullable View heroCard, int extraDp) {
        if (heroCard == null) return;
        final int extra = dp(heroCard, extraDp);
        ViewGroup.LayoutParams params = heroCard.getLayoutParams();
        if (!(params instanceof ViewGroup.MarginLayoutParams)) {
            applyTopInsetPadding(heroCard, extraDp);
            return;
        }
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) params;
        final int baseTop = lp.topMargin;
        ViewCompat.setOnApplyWindowInsetsListener(heroCard, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            ViewGroup.LayoutParams p = v.getLayoutParams();
            if (p instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) p;
                mlp.topMargin = baseTop + topInset + extra;
                v.setLayoutParams(mlp);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(heroCard);
    }

    private static int dp(@NonNull View view, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, view.getResources().getDisplayMetrics()));
    }
}
