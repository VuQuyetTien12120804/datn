package com.example.frontend_bookingcare.ui.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.PatientAppointmentDto;
import com.example.frontend_bookingcare.locale.LocaleStore;
import com.example.frontend_bookingcare.ui.booking.BookingFormatters;
import com.example.frontend_bookingcare.ui.booking.BookingPolicy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class NotificationMapper {

    public enum Category {
        APPOINTMENT,
        HEALTH
    }

    public enum Kind {
        BOOKING_SUCCESS,
        REMINDER
    }

    public static final class Item {
        public final Kind kind;
        public final Category category;
        public final String title;
        public final String body;
        public final String timeLabel;
        public final long sortMs;
        public final int appointmentId;
        public final int doctorId;
        @Nullable public final String doctorName;
        @Nullable public final String date;
        @Nullable public final String start;
        @Nullable public final String end;
        @Nullable public final String status;

        Item(Kind kind, Category category, String title, String body, String timeLabel, long sortMs,
             int appointmentId, int doctorId, @Nullable String doctorName,
             @Nullable String date, @Nullable String start, @Nullable String end, @Nullable String status) {
            this.kind = kind;
            this.category = category;
            this.title = title;
            this.body = body;
            this.timeLabel = timeLabel;
            this.sortMs = sortMs;
            this.appointmentId = appointmentId;
            this.doctorId = doctorId;
            this.doctorName = doctorName;
            this.date = date;
            this.start = start;
            this.end = end;
            this.status = status;
        }
    }

    private static final long REMINDER_WINDOW_MS = 24L * 60L * 60L * 1000L;

    private NotificationMapper() {
    }

    @NonNull
    public static List<Item> fromAppointments(@NonNull Context ctx, @Nullable List<PatientAppointmentDto> upcoming) {
        List<Item> out = new ArrayList<>();
        if (upcoming == null) return out;
        for (PatientAppointmentDto dto : upcoming) {
            if (dto == null || dto.appointmentId == null) continue;
            Item item = toItem(ctx, dto);
            if (item != null) out.add(item);
        }
        Collections.sort(out, Comparator.comparingLong((Item i) -> i.sortMs));
        return out;
    }

    @Nullable
    private static Item toItem(@NonNull Context ctx, @NonNull PatientAppointmentDto dto) {
        long sortMs = parseSortMs(dto.appointmentDate, dto.startTime);
        String status = dto.status != null ? dto.status.trim().toUpperCase(Locale.ROOT) : "";
        Kind kind = resolveKind(status, sortMs);
        String doctor = dto.doctorName != null ? dto.doctorName : ctx.getString(R.string.dash_placeholder);
        String timeText = formatDisplayTime(ctx, dto.startTime);
        String dateText = formatDisplayDate(dto.appointmentDate);
        String title = kind == Kind.BOOKING_SUCCESS
                ? ctx.getString(R.string.notification_booking_success_title)
                : ctx.getString(R.string.notification_reminder_title);
        int bodyRes = kind == Kind.BOOKING_SUCCESS
                ? R.string.notification_success_body_fmt
                : R.string.notification_reminder_body_fmt;
        String body = ctx.getString(bodyRes, doctor, timeText, dateText);
        String timeLabel = buildTimeLabel(ctx, dto.startTime, dto.appointmentDate);
        return new Item(
                kind,
                Category.APPOINTMENT,
                title,
                body,
                timeLabel,
                sortMs,
                dto.appointmentId,
                dto.doctorId != null ? dto.doctorId : 0,
                dto.doctorName,
                dto.appointmentDate,
                dto.startTime,
                dto.endTime,
                dto.status
        );
    }

    private static Kind resolveKind(@NonNull String status, long sortMs) {
        if ("PENDING".equals(status)) return Kind.BOOKING_SUCCESS;
        long now = System.currentTimeMillis();
        if ("CONFIRMED".equals(status) && sortMs > 0 && sortMs - now > REMINDER_WINDOW_MS) {
            return Kind.BOOKING_SUCCESS;
        }
        return Kind.REMINDER;
    }

    private static String formatDisplayTime(@NonNull Context ctx, @Nullable String raw) {
        String t = BookingFormatters.shortTime(raw);
        if (t.isEmpty()) return ctx.getString(R.string.dash_placeholder);
        if (LocaleStore.isEnglish(ctx)) return t;
        return t.replace(':', 'h');
    }

    private static String formatDisplayDate(@Nullable String isoDate) {
        LocalDate d = BookingFormatters.parseIsoDate(isoDate);
        if (d != null) return d.format(BookingFormatters.DISPLAY_DATE);
        return isoDate != null ? isoDate : "—";
    }

    private static String buildTimeLabel(@NonNull Context ctx, @Nullable String start, @Nullable String isoDate) {
        String t = formatDisplayTime(ctx, start);
        String d = formatDisplayDate(isoDate);
        return ctx.getString(R.string.notification_time_label_fmt, t, d);
    }

    private static long parseSortMs(@Nullable String isoDate, @Nullable String start) {
        LocalDate d = BookingFormatters.parseIsoDate(isoDate);
        if (d == null) return 0L;
        LocalTime time = BookingFormatters.parseTime(start);
        if (time == null) time = LocalTime.MIDNIGHT;
        return LocalDateTime.of(d, time).atZone(BookingPolicy.CLINIC_ZONE).toInstant().toEpochMilli();
    }
}
