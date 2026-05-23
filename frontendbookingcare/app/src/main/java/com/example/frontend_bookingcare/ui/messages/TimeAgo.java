package com.example.frontend_bookingcare.ui.messages;

import android.content.Context;

import com.example.frontend_bookingcare.BookingCareApp;
import com.example.frontend_bookingcare.R;

public final class TimeAgo {
    private TimeAgo() {
    }

    public static String format(Context ctx, long nowMs, long tsMs) {
        if (tsMs <= 0) return "";
        long diff = Math.max(0, nowMs - tsMs);
        long sec = diff / 1000L;
        if (sec < 10) return ctx.getString(R.string.time_ago_just_now);
        if (sec < 60) return ctx.getString(R.string.time_ago_seconds_fmt, sec);
        long min = sec / 60L;
        if (min < 60) return ctx.getString(R.string.time_ago_minutes_fmt, min);
        long hr = min / 60L;
        if (hr < 24) return ctx.getString(R.string.time_ago_hours_fmt, hr);
        long day = hr / 24L;
        if (day < 7) return ctx.getString(R.string.time_ago_days_fmt, day);
        long wk = day / 7L;
        if (wk < 5) return ctx.getString(R.string.time_ago_weeks_fmt, wk);
        long mo = day / 30L;
        if (mo < 12) return ctx.getString(R.string.time_ago_months_fmt, mo);
        long yr = day / 365L;
        return ctx.getString(R.string.time_ago_years_fmt, yr);
    }

    /** Doctor panel / legacy callers without a {@link Context}. */
    public static String format(long nowMs, long tsMs) {
        Context ctx = BookingCareApp.appContext();
        return ctx != null ? format(ctx, nowMs, tsMs) : "";
    }
}
