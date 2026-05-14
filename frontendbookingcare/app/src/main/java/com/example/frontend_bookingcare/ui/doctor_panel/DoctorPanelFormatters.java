package com.example.frontend_bookingcare.ui.doctor_panel;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.example.frontend_bookingcare.api.DoctorAppointmentDto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

final class DoctorPanelFormatters {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final SimpleDateFormat ISO_DATE = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DOB_DMY_SLASH = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
    private static final SimpleDateFormat DOB_DMY_DASH = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private static final SimpleDateFormat DISPLAY_DATE = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private DoctorPanelFormatters() {
    }

    static String displayGender(String g) {
        if (TextUtils.isEmpty(g)) return "—";
        switch (g.trim().toUpperCase(Locale.ROOT)) {
            case "MALE":
            case "NAM":
                return "Nam";
            case "FEMALE":
            case "NU":
            case "NỮ":
                return "Nữ";
            default:
                return g.trim();
        }
    }

    /**
     * Tuổi hiển thị cho panel bác sĩ: ưu tiên tính từ ngày sinh; nếu không có/không parse được thì dùng tuổi API (nếu có).
     */
    static int resolvePatientAge(JsonElement dob, Integer patientAgeFromApi) {
        int fromDob = ageFromDob(dob);
        if (fromDob > 0) return fromDob;
        if (patientAgeFromApi != null && patientAgeFromApi > 0 && patientAgeFromApi < 150) {
            return patientAgeFromApi;
        }
        return 0;
    }

    static int ageFromDob(JsonElement dob) {
        Calendar birth = parseBirthCalendar(dob);
        if (birth == null) return 0;
        Calendar now = Calendar.getInstance();
        int years = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            years--;
        }
        return Math.max(0, years);
    }

    /** Tuổi từ chuỗi ngày API (vd. {@code patientDob} trong chi tiết lịch). */
    static int ageFromDateString(String raw) {
        if (TextUtils.isEmpty(raw)) return 0;
        Calendar birth = parseBirthString(raw.trim());
        if (birth == null) return 0;
        Calendar now = Calendar.getInstance();
        int years = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            years--;
        }
        return Math.max(0, years);
    }

    static String formatAppointmentDate(String iso) {
        if (TextUtils.isEmpty(iso)) return "—";
        try {
            synchronized (ISO_DATE) {
                Calendar c = Calendar.getInstance();
                c.setTime(ISO_DATE.parse(iso));
                synchronized (DISPLAY_DATE) {
                    return DISPLAY_DATE.format(c.getTime());
                }
            }
        } catch (ParseException e) {
            return iso;
        }
    }

    static String formatTimeLabel(JsonElement expectedTime) {
        if (expectedTime == null || expectedTime.isJsonNull()) return "—";
        if (expectedTime.isJsonArray()) {
            JsonArray a = expectedTime.getAsJsonArray();
            if (a.size() >= 2) {
                int h = a.get(0).getAsInt();
                int m = a.get(1).getAsInt();
                return String.format(Locale.getDefault(), "%02d:%02d", h, m);
            }
            if (a.size() == 1) {
                int h = a.get(0).getAsInt();
                return String.format(Locale.getDefault(), "%02d:00", h);
            }
            return "—";
        }
        if (expectedTime.isJsonPrimitive()) {
            String s = expectedTime.getAsString();
            if (s.length() >= 5) return s.substring(0, 5);
            return s;
        }
        return "—";
    }

    static String formatTimeAmPm(JsonElement expectedTime) {
        String hm = formatTimeLabel(expectedTime);
        if ("—".equals(hm) || hm.length() < 4) return hm;
        try {
            String[] parts = hm.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int hour12 = h % 12;
            if (hour12 == 0) hour12 = 12;
            String suffix = h < 12 ? "AM" : "PM";
            return String.format(Locale.getDefault(), "%d:%02d %s", hour12, m, suffix);
        } catch (Exception e) {
            return hm;
        }
    }

    /**
     * Thời điểm bắt đầu khám (epoch millis, múi giờ phòng khám) từ ngày API + giờ dự kiến.
     */
    @Nullable
    static Long appointmentSlotStartMillis(@Nullable String appointmentDateIso, @Nullable JsonElement expectedTime) {
        if (TextUtils.isEmpty(appointmentDateIso)) return null;
        LocalDate date;
        try {
            date = LocalDate.parse(appointmentDateIso.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
        LocalTime time = localTimeFromExpected(expectedTime);
        if (time == null) return null;
        return LocalDateTime.of(date, time).atZone(CLINIC_ZONE).toInstant().toEpochMilli();
    }

    /** Ngày khám (yyyy-MM-dd) đã trước “hôm nay” theo múi phòng khám (chưa xét giờ). */
    static boolean isStrictlyPastAppointmentDay(@Nullable String appointmentDateIso) {
        if (TextUtils.isEmpty(appointmentDateIso)) return false;
        try {
            LocalDate d = LocalDate.parse(appointmentDateIso.trim());
            LocalDate today = LocalDate.now(CLINIC_ZONE);
            return d.isBefore(today);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /** Giống logic quá hạn PENDING trên tab Yêu cầu (giờ/ngày theo múi phòng khám). */
    static boolean isPastPendingSlot(@Nullable DoctorAppointmentDto d) {
        if (d == null) return false;
        Long ms = appointmentSlotStartMillis(d.appointmentDate, d.expectedTime);
        if (ms != null) {
            return ms < System.currentTimeMillis();
        }
        return isStrictlyPastAppointmentDay(d.appointmentDate);
    }

    /** Số lịch PENDING còn có thể duyệt (chưa quá giờ bắt đầu slot). */
    static int countActionablePending(@Nullable List<DoctorAppointmentDto> pending) {
        if (pending == null) return 0;
        int n = 0;
        for (DoctorAppointmentDto d : pending) {
            if (!isPastPendingSlot(d)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Hiển thị ô «Đã khám hôm nay»: đã hoàn thành / tổng lịch trong ngày (API today), không đếm CANCELLED.
     */
    static String formatTodayCompletedRatio(@Nullable List<DoctorAppointmentDto> today) {
        if (today == null || today.isEmpty()) {
            return "0/0";
        }
        int total = 0;
        int completed = 0;
        for (DoctorAppointmentDto d : today) {
            if (d == null) continue;
            String st = appointmentStatusUpper(d.status);
            if ("CANCELLED".equals(st)) {
                continue;
            }
            total++;
            if ("COMPLETED".equals(st)) {
                completed++;
            }
        }
        return completed + "/" + total;
    }

    private static String appointmentStatusUpper(@Nullable String status) {
        if (status == null) return "";
        return status.trim().toUpperCase(Locale.ROOT);
    }

    @Nullable
    private static LocalTime localTimeFromExpected(@Nullable JsonElement expectedTime) {
        if (expectedTime == null || expectedTime.isJsonNull()) return null;
        if (expectedTime.isJsonArray()) {
            JsonArray a = expectedTime.getAsJsonArray();
            if (a.size() >= 2) {
                int h = a.get(0).getAsInt();
                int m = a.get(1).getAsInt();
                return LocalTime.of(h, m);
            }
            if (a.size() == 1) {
                return LocalTime.of(a.get(0).getAsInt(), 0);
            }
            return null;
        }
        if (expectedTime.isJsonPrimitive()) {
            String s = expectedTime.getAsString().trim();
            if (TextUtils.isEmpty(s)) return null;
            if (s.length() >= 8 && s.charAt(2) == ':' && s.charAt(5) == ':') {
                try {
                    return LocalTime.parse(s.substring(0, 8), DateTimeFormatter.ofPattern("HH:mm:ss"));
                } catch (Exception ignored) {
                }
            }
            if (s.length() >= 5) {
                try {
                    return LocalTime.parse(s.substring(0, 5), DateTimeFormatter.ofPattern("HH:mm"));
                } catch (Exception ignored) {
                }
            }
            try {
                return LocalTime.parse(s);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Calendar parseBirthCalendar(JsonElement dob) {
        if (dob == null || dob.isJsonNull()) return null;
        if (dob.isJsonArray()) {
            JsonArray a = dob.getAsJsonArray();
            if (a.size() >= 3) {
                int y = a.get(0).getAsInt();
                int mo = a.get(1).getAsInt() - 1;
                int day = a.get(2).getAsInt();
                return calendarAtMidnight(y, mo, day);
            }
            return null;
        }
        if (dob.isJsonObject()) {
            return parseBirthObject(dob.getAsJsonObject());
        }
        if (dob.isJsonPrimitive()) {
            JsonPrimitive p = dob.getAsJsonPrimitive();
            if (p.isNumber()) {
                double n = p.getAsDouble();
                if (n > 1e12) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis((long) n);
                    return calendarAtMidnight(
                            c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH));
                }
                return null;
            }
            return parseBirthString(p.getAsString());
        }
        return null;
    }

    private static Calendar parseBirthObject(JsonObject o) {
        if (!o.has("year")) return null;
        int y = o.get("year").getAsInt();
        int mo;
        int day;
        if (o.has("monthValue")) {
            mo = o.get("monthValue").getAsInt() - 1;
        } else if (o.has("month") && o.get("month").isJsonPrimitive() && o.get("month").getAsJsonPrimitive().isNumber()) {
            mo = o.get("month").getAsInt() - 1;
        } else {
            return null;
        }
        if (o.has("dayOfMonth")) {
            day = o.get("dayOfMonth").getAsInt();
        } else if (o.has("day")) {
            day = o.get("day").getAsInt();
        } else {
            return null;
        }
        if (mo < 0 || mo > 11 || day < 1 || day > 31) return null;
        return calendarAtMidnight(y, mo, day);
    }

    private static Calendar calendarAtMidnight(int year, int monthZeroBased, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, monthZeroBased);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private static Calendar parseBirthString(String raw) {
        if (TextUtils.isEmpty(raw)) return null;
        String s = raw.trim();
        int t = s.indexOf('T');
        if (t == 10 && s.length() > 10) {
            s = s.substring(0, 10);
        } else if (t > 0 && t < s.length()) {
            s = s.substring(0, t);
        }
        if (s.length() >= 10
                && Character.isDigit(s.charAt(0))
                && s.regionMatches(4, "-", 0, 1)) {
            try {
                synchronized (ISO_DATE) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(ISO_DATE.parse(s.substring(0, 10)));
                    return calendarAtMidnight(
                            c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH));
                }
            } catch (ParseException ignored) {
            }
        }
        try {
            synchronized (DOB_DMY_SLASH) {
                Calendar c = Calendar.getInstance();
                c.setTime(DOB_DMY_SLASH.parse(s));
                return calendarAtMidnight(
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH));
            }
        } catch (ParseException ignored) {
        }
        try {
            synchronized (DOB_DMY_DASH) {
                Calendar c = Calendar.getInstance();
                c.setTime(DOB_DMY_DASH.parse(s));
                return calendarAtMidnight(
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH));
            }
        } catch (ParseException ignored) {
        }
        return null;
    }
}
