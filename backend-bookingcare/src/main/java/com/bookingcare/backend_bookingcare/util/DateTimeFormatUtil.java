package com.bookingcare.backend_bookingcare.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateTimeFormatUtil {

    public static final ZoneOffset CLINIC_OFFSET = ZoneOffset.ofHours(7);

    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateTimeFormatUtil() {
    }

    /** Giữ nguyên giờ địa phương phòng khám (+07:00), tránh JDBC ghi UTC làm lệch 7 giờ. */
    public static OffsetDateTime toClinicOffset(OffsetDateTime dt) {
        if (dt == null) return null;
        return dt.atZoneSameInstant(CLINIC_OFFSET).toLocalDateTime().atOffset(CLINIC_OFFSET);
    }

    public static String toIsoDate(OffsetDateTime dt) {
        if (dt == null) return null;
        return dt.atZoneSameInstant(CLINIC_OFFSET).toLocalDate().format(ISO_DATE);
    }

    public static String toTimeHm(OffsetDateTime dt) {
        if (dt == null) return null;
        return dt.atZoneSameInstant(CLINIC_OFFSET).toLocalTime().format(HH_MM);
    }

    public static String toDdMmYyyy(LocalDate date) {
        if (date == null) return null;
        return date.format(DD_MM_YYYY);
    }

    public static LocalDate parseDdMmYyyy(String s) {
        if (s == null || s.isBlank()) return null;
        if (s.contains("/")) {
            return LocalDate.parse(s, DD_MM_YYYY);
        }
        return LocalDate.parse(s, ISO_DATE);
    }

    public static LocalDate parseIsoDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s, ISO_DATE);
    }

    public static OffsetDateTime dayStart(LocalDate day) {
        return day.atStartOfDay().atOffset(CLINIC_OFFSET);
    }

    public static OffsetDateTime dayEnd(LocalDate day) {
        return day.plusDays(1).atStartOfDay().atOffset(CLINIC_OFFSET);
    }
}
