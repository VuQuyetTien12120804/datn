package com.example.frontend_bookingcare.ui.booking;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Quy tắc đặt lịch phía app (backend nên kiểm tra lại).
 * Giờ tính theo {@link #CLINIC_ZONE} để khớp khung giờ phòng khám.
 */
public final class BookingPolicy {

    /** Khách phải đặt trước giờ bắt đầu slot ít nhất bấy nhiêu phút. */
    public static final int MIN_LEAD_MINUTES = 30;

    public static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private BookingPolicy() {
    }

    /**
     * @return epoch millis bắt đầu slot, hoặc null nếu không parse được.
     */
    @Nullable
    public static Long slotStartMillis(@Nullable String isoDate, @Nullable String startTime) {
        LocalDate d = BookingFormatters.parseIsoDate(isoDate);
        LocalTime t = BookingFormatters.parseTime(startTime);
        if (d == null || t == null) return null;
        return LocalDateTime.of(d, t).atZone(CLINIC_ZONE).toInstant().toEpochMilli();
    }

    /**
     * Slot không đủ thời gian dự phòng so với thời điểm hiện tại (theo {@link #CLINIC_ZONE}).
     */
    public static boolean violatesMinLead(@Nullable String isoDate, @Nullable String startTime) {
        Long ms = slotStartMillis(isoDate, startTime);
        if (ms == null) return false;
        long minStart = Instant.now().toEpochMilli() + MIN_LEAD_MINUTES * 60_000L;
        return ms < minStart;
    }
}
