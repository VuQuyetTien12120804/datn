package com.example.frontend_bookingcare.onboarding;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lưu cờ đã xem onboarding để các lần mở app sau không hiện lại.
 */
public final class OnboardingStore {

    private static final String PREFS = "bookingcare_app_settings";
    private static final String KEY_DONE = "onboarding_done";

    private OnboardingStore() {
    }

    public static boolean isDone(Context context) {
        return prefs(context).getBoolean(KEY_DONE, false);
    }

    public static void markDone(Context context) {
        prefs(context).edit().putBoolean(KEY_DONE, true).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
