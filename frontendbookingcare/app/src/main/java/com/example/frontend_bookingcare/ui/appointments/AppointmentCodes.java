package com.example.frontend_bookingcare.ui.appointments;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Mã phiếu khám / mã bệnh nhân thống nhất trên Result, Ticket và danh sách lịch. */
public final class AppointmentCodes {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private AppointmentCodes() {
    }

    public static String appointmentCode(int appointmentId, @Nullable String isoDate) {
        return "YMA" + yyMmDd(isoDate) + String.format(Locale.US, "%04d", Math.max(0, appointmentId));
    }

    public static String patientCode(int patientId, @Nullable String isoDate) {
        String day = yyMmDd(isoDate);
        if (patientId <= 0) {
            return "YMP" + day + "----";
        }
        return "YMP" + day + String.format(Locale.US, "%04d", patientId);
    }

    private static String yyMmDd(@Nullable String isoDate) {
        if (isoDate != null && isoDate.length() >= 10) {
            try {
                return LocalDate.parse(isoDate.substring(0, 10))
                        .format(DateTimeFormatter.ofPattern("yyMMdd"));
            } catch (Exception ignored) {
            }
        }
        return LocalDate.now(CLINIC_ZONE).format(DateTimeFormatter.ofPattern("yyMMdd"));
    }
}
