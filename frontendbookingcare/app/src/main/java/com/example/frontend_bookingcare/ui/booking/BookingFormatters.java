package com.example.frontend_bookingcare.ui.booking;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.locale.LocaleStore;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Các helper format / parse ngày giờ dùng xuyên suốt luồng booking.
 * - Backend API nhận/trả ISO: date "yyyy-MM-dd", time "HH:mm:ss".
 * - UI hiển thị: date "T{n} dd/MM/yyyy", time "HH:mm".
 * - Profile ngày sinh hiện dùng "dd/MM/yyyy" (theo EditProfileFragment).
 */
public final class BookingFormatters {

    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter SHORT_TIME = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DOB_VN = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private BookingFormatters() {
    }

    @Nullable
    public static LocalDate parseIsoDate(@Nullable String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return LocalDate.parse(s, ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static LocalTime parseTime(@Nullable String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            // Chấp nhận "HH:mm" hoặc "HH:mm:ss".
            if (s.length() <= 5) return LocalTime.parse(s, SHORT_TIME);
            return LocalTime.parse(s, ISO_TIME);
        } catch (Exception e) {
            try {
                return LocalTime.parse(s);
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    /** "yyyy-MM-dd" → "T{n} dd/MM/yyyy" (VN) hoặc "Mon dd/MM/yyyy" (EN). */
    public static String prettyDate(Context ctx, @Nullable String isoDate) {
        LocalDate d = parseIsoDate(isoDate);
        if (d == null) return isoDate == null ? "" : isoDate;
        return dowLabel(ctx, d) + " " + d.format(DISPLAY_DATE);
    }

    /** Thứ trong tuần theo locale app. */
    public static String dowLabel(Context ctx, LocalDate d) {
        if (LocaleStore.isEnglish(ctx)) {
            return d.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH));
        }
        int v = d.getDayOfWeek().getValue();
        if (v == 7) return ctx.getString(R.string.dow_sun);
        return ctx.getString(dowResFor(v));
    }

    @Deprecated
    public static String prettyDate(@Nullable String isoDate) {
        Context ctx = com.example.frontend_bookingcare.BookingCareApp.appContext();
        return ctx != null ? prettyDate(ctx, isoDate) : prettyDateLegacy(isoDate);
    }

    @Deprecated
    public static String dowLabel(LocalDate d) {
        Context ctx = com.example.frontend_bookingcare.BookingCareApp.appContext();
        return ctx != null ? dowLabel(ctx, d) : dowLabelLegacy(d);
    }

    private static String prettyDateLegacy(@Nullable String isoDate) {
        LocalDate d = parseIsoDate(isoDate);
        if (d == null) return isoDate == null ? "" : isoDate;
        return dowLabelLegacy(d) + " " + d.format(DISPLAY_DATE);
    }

    private static String dowLabelLegacy(LocalDate d) {
        int v = d.getDayOfWeek().getValue();
        if (v == 7) return "CN";
        return "T" + (v + 1);
    }

    private static int dowResFor(int dayOfWeekValue) {
        switch (dayOfWeekValue) {
            case 1: return R.string.dow_mon;
            case 2: return R.string.dow_tue;
            case 3: return R.string.dow_wed;
            case 4: return R.string.dow_thu;
            case 5: return R.string.dow_fri;
            case 6: return R.string.dow_sat;
            default: return R.string.dow_sun;
        }
    }

    /** "HH:mm:ss" → "HH:mm". */
    public static String shortTime(@Nullable String rawTime) {
        LocalTime t = parseTime(rawTime);
        return t == null ? (rawTime == null ? "" : rawTime) : t.format(SHORT_TIME);
    }

    /** "08:30-09:00" style. */
    public static String timeRange(@Nullable String startRaw, @Nullable String endRaw) {
        String s = shortTime(startRaw);
        String e = shortTime(endRaw);
        if (s.isEmpty() && e.isEmpty()) return "";
        if (e.isEmpty()) return s;
        if (s.isEmpty()) return e;
        return s + "-" + e;
    }

    /** Trả "morning" nếu giờ bắt đầu < 12:00, ngược lại "afternoon". */
    public static String sessionOf(@Nullable String rawStart) {
        LocalTime t = parseTime(rawStart);
        if (t == null) return "morning";
        return t.getHour() < 12 ? "morning" : "afternoon";
    }

    /** "dd/MM/yyyy" → "yyyy-MM-dd" (trả null nếu không parse được). */
    @Nullable
    public static String dobVnToIso(@Nullable String vnDob) {
        if (vnDob == null || vnDob.isEmpty()) return null;
        try {
            LocalDate d = LocalDate.parse(vnDob.trim(), DOB_VN);
            return d.format(ISO_DATE);
        } catch (Exception e) {
            // Có thể người dùng đã nhập yyyy-MM-dd sẵn.
            try {
                LocalDate d = LocalDate.parse(vnDob.trim(), ISO_DATE);
                return d.format(ISO_DATE);
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    /** Map "Nam"/"Nữ"/"Khác" → enum backend ("MALE"/"FEMALE"/"OTHER"). */
    public static String genderToEnum(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return "OTHER";
        String n = raw.trim().toLowerCase(Locale.ROOT);
        if (n.contains("nam") || n.equals("male") || n.equals("m")) return "MALE";
        if (n.contains("nữ") || n.contains("nu") || n.equals("female") || n.equals("f")) return "FEMALE";
        return "OTHER";
    }

    /** "MALE" → nhãn hiển thị theo locale. */
    public static String displayGender(Context ctx, @Nullable String raw) {
        String v = genderToEnum(raw);
        switch (v) {
            case "MALE":
                return ctx.getString(R.string.gender_male);
            case "FEMALE":
                return ctx.getString(R.string.gender_female);
            default:
                return ctx.getString(R.string.gender_other);
        }
    }

    public static String[] genderLabels(Context ctx) {
        return new String[]{
                ctx.getString(R.string.gender_male),
                ctx.getString(R.string.gender_female),
                ctx.getString(R.string.gender_other)
        };
    }

    public static boolean isValidVnPhone(@Nullable String raw) {
        if (raw == null) return false;
        String p = raw.trim().replaceAll("\\s+", "");
        return p.matches("0\\d{9}") || p.matches("\\+84\\d{9}");
    }

    public static boolean isValidDobDdMmYyyy(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return false;
        try {
            LocalDate d = LocalDate.parse(raw.trim(), DOB_VN);
            return !d.isAfter(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }
}
