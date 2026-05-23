package com.example.frontend_bookingcare.session;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.onboarding.OnboardingActivity;

/**
 * Đăng ký một lần trong {@link com.example.frontend_bookingcare.BookingCareApp} để mọi
 * Activity (booking, chat, vé khám, …) đều quay về tab Tài khoản khi refresh token hết hạn.
 */
public final class SessionExpiredNavigator {

    private static volatile boolean installed;

    private SessionExpiredNavigator() {
    }

    public static void install(Context appContext) {
        if (installed) return;
        synchronized (SessionExpiredNavigator.class) {
            if (installed) return;
            installed = true;
            SessionExpiredBus.register(() -> {
                Context ctx = appContext.getApplicationContext();
                Toast.makeText(ctx, R.string.session_expired, Toast.LENGTH_LONG).show();
                Intent i = new Intent(ctx, MainActivity.class);
                i.putExtra(OnboardingActivity.EXTRA_OPEN_ACCOUNT, true);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                ctx.startActivity(i);
            });
        }
    }
}
