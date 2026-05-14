package com.example.frontend_bookingcare.ui.messages;

public final class TimeAgo {
    private TimeAgo() {
    }

    public static String format(long nowMs, long tsMs) {
        if (tsMs <= 0) return "";
        long diff = Math.max(0, nowMs - tsMs);
        long sec = diff / 1000L;
        if (sec < 10) return "Vừa xong";
        if (sec < 60) return sec + " giây";
        long min = sec / 60L;
        if (min < 60) return min + " phút";
        long hr = min / 60L;
        if (hr < 24) return hr + " giờ";
        long day = hr / 24L;
        if (day < 7) return day + " ngày";
        long wk = day / 7L;
        if (wk < 5) return wk + " tuần";
        long mo = day / 30L;
        if (mo < 12) return mo + " tháng";
        long yr = day / 365L;
        return yr + " năm";
    }
}

