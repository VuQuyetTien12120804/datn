package com.example.frontend_bookingcare.util;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.frontend_bookingcare.BookingCareApp;
import com.example.frontend_bookingcare.R;

/** Localized fallback messages for repositories without a UI {@link Context}. */
public final class RepoMessages {

    private RepoMessages() {
    }

    public static String networkError() {
        Context ctx = BookingCareApp.appContext();
        return ctx != null ? ctx.getString(R.string.error_network_generic) : "Network error";
    }

    public static String httpError(int httpCode) {
        Context ctx = BookingCareApp.appContext();
        return ctx != null ? ctx.getString(R.string.error_network_fmt, httpCode) : ("HTTP " + httpCode);
    }

    public static String profileParseFailed() {
        Context ctx = BookingCareApp.appContext();
        return ctx != null ? ctx.getString(R.string.error_profile_parse_failed) : "Profile parse error";
    }
}
