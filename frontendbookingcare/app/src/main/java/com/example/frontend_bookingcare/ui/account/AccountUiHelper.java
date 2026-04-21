package com.example.frontend_bookingcare.ui.account;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.frontend_bookingcare.R;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Nút quay lại trên app bar trong {@code account_inner_container} phải pop
 * {@link AccountFragment#getChildFragmentManager()}. Luôn dùng child FM của {@link AccountFragment}
 * khi fragment con có parent là Account — tránh lệch FM so với nơi thực sự {@code addToBackStack}.
 * Layout có thể dùng {@code R.id.auth_manual_back} (ImageButton) cạnh {@link MaterialToolbar} để
 * nhận chạm ổn định hơn icon navigation mặc định.
 */
public final class AccountUiHelper {

    private AccountUiHelper() {
    }

    /**
     * Pop một mục trên back stack của luồng con Tài khoản.
     */
    public static boolean popAccountInnerBack(@NonNull Fragment fragment) {
        FragmentManager fm = resolveAccountInnerFragmentManager(fragment);
        try {
            fm.executePendingTransactions();
        } catch (IllegalStateException ignored) {
        }
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();
            return true;
        }
        return false;
    }

    /**
     * Pop back stack con nếu có; nếu không có thì fallback về back hệ thống (Activity).
     * Tránh cảm giác "không click được" khi thực ra click có nhưng không có gì để pop.
     */
    public static void navigateUp(@NonNull Fragment fragment) {
        if (!popAccountInnerBack(fragment)) {
            fragment.requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    }

    private static int dp(Context c, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics()));
    }

    /**
     * Đẩy vùng app bar xuống dưới status bar/camera cutout (theo WindowInsets).
     * Áp dụng lên container của toolbar (thường là FrameLayout).
     */
    private static void applyTopInsetPadding(@NonNull Fragment fragment, @NonNull View container) {
        final int extra = dp(fragment.requireContext(), 6); // thêm "một chút" dưới camera/notch
        final int startPad = container.getPaddingStart();
        final int endPad = container.getPaddingEnd();
        final int bottomPad = container.getPaddingBottom();
        final int baseTop = container.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPaddingRelative(startPad, baseTop + topInset + extra, endPad, bottomPad);
            return insets;
        });
        ViewCompat.requestApplyInsets(container);
    }

    @NonNull
    private static FragmentManager resolveAccountInnerFragmentManager(@NonNull Fragment fragment) {
        Fragment parent = fragment.getParentFragment();
        if (parent instanceof AccountFragment) {
            return parent.getChildFragmentManager();
        }
        return fragment.getParentFragmentManager();
    }

    /**
     * Gắn nút quay lại: ưu tiên {@code auth_manual_back} cùng parent với toolbar; không có thì dùng
     * {@link MaterialToolbar#setNavigationOnClickListener}.
     */
    public static void bindToolbarBack(@NonNull Fragment fragment, @NonNull MaterialToolbar toolbar) {
        ViewParent vp = toolbar.getParent();
        if (vp instanceof View) {
            View parent = (View) vp;
            applyTopInsetPadding(fragment, parent);
            View manual = parent.findViewById(R.id.auth_manual_back);
            if (manual != null) {
                // Defensive: một số layout/toolbar có thể vẽ đè khiến nút không nhận touch.
                manual.setClickable(true);
                manual.setFocusable(true);
                manual.bringToFront();
                manual.setOnClickListener(v -> navigateUp(fragment));
                toolbar.setNavigationIcon(null);
                toolbar.setNavigationOnClickListener(null);
                return;
            }
        }
        toolbar.setNavigationIcon(AppCompatResources.getDrawable(fragment.requireContext(), R.drawable.ic_auth_back));
        toolbar.setNavigationContentDescription(fragment.getString(R.string.cd_navigate_up));
        toolbar.setNavigationOnClickListener(v -> navigateUp(fragment));
    }
}
