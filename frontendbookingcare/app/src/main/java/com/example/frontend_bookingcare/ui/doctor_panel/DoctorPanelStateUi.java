package com.example.frontend_bookingcare.ui.doctor_panel;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;

/**
 * Loading / error overlay dùng chung cho các tab doctor panel.
 */
public final class DoctorPanelStateUi {

    private DoctorPanelStateUi() {
    }

    public static void showLoading(@Nullable ProgressBar loading,
                                   @Nullable View errorPanel,
                                   @Nullable SwipeRefreshLayout refresh) {
        if (refresh != null && refresh.isRefreshing()) {
            if (errorPanel != null) errorPanel.setVisibility(View.GONE);
            return;
        }
        if (loading != null) loading.setVisibility(View.VISIBLE);
        if (errorPanel != null) errorPanel.setVisibility(View.GONE);
    }

    public static void showError(@Nullable ProgressBar loading,
                                 @Nullable View errorPanel,
                                 @Nullable TextView errorText,
                                 @Nullable MaterialButton retryBtn,
                                 @Nullable SwipeRefreshLayout refresh,
                                 @NonNull String message,
                                 @NonNull Runnable onRetry) {
        if (refresh != null) refresh.setRefreshing(false);
        if (loading != null) loading.setVisibility(View.GONE);
        if (errorPanel != null) errorPanel.setVisibility(View.VISIBLE);
        if (errorText != null) errorText.setText(message);
        if (retryBtn != null) retryBtn.setOnClickListener(v -> onRetry.run());
    }

    public static void hide(@Nullable ProgressBar loading,
                            @Nullable View errorPanel,
                            @Nullable SwipeRefreshLayout refresh) {
        if (refresh != null) refresh.setRefreshing(false);
        if (loading != null) loading.setVisibility(View.GONE);
        if (errorPanel != null) errorPanel.setVisibility(View.GONE);
    }
}
