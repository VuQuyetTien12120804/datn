package com.example.frontend_bookingcare.ui.common;

import android.util.TypedValue;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

public final class HeaderInsets {
    private HeaderInsets() {
    }

    /**
     * Apply status-bar inset to a toolbar so the background color
     * extends behind the status bar and the content stays centered.
     */
    public static void applyToToolbar(MaterialToolbar toolbar) {
        if (toolbar == null) return;
        final int basePadTop = toolbar.getPaddingTop();
        final int basePadBottom = toolbar.getPaddingBottom();
        final int basePadStart = toolbar.getPaddingStart();
        final int basePadEnd = toolbar.getPaddingEnd();

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPaddingRelative(basePadStart, basePadTop + topInset, basePadEnd, basePadBottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(toolbar);
    }

    // no extra padding: keep header height consistent across devices
}

