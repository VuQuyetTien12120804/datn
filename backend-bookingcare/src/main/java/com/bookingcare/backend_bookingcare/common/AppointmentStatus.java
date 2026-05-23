package com.bookingcare.backend_bookingcare.common;

import java.util.Locale;
import java.util.Set;

public final class AppointmentStatus {

    public static final String PENDING = "pending";
    public static final String CONFIRMED = "confirmed";
    public static final String CHECKED_IN = "checked_in";
    public static final String COMPLETED = "completed";
    public static final String CANCELLED = "cancelled";
    public static final String NO_SHOW = "no_show";

    private static final Set<String> TERMINAL = Set.of(COMPLETED, CANCELLED, NO_SHOW);

    private AppointmentStatus() {
    }

    public static String normalize(String status) {
        if (status == null || status.isBlank()) {
            return PENDING;
        }
        return status.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isTerminal(String status) {
        return TERMINAL.contains(normalize(status));
    }

    public static boolean canDoctorConfirm(String status) {
        return PENDING.equals(normalize(status));
    }

    public static boolean canDoctorCancel(String status) {
        String s = normalize(status);
        return PENDING.equals(s) || CONFIRMED.equals(s);
    }

    public static boolean canPatientCheckIn(String status) {
        return CONFIRMED.equals(normalize(status));
    }

    public static boolean canDoctorStartExam(String status) {
        String s = normalize(status);
        return CONFIRMED.equals(s) || CHECKED_IN.equals(s);
    }

    public static boolean canDoctorSaveNote(String status) {
        return CHECKED_IN.equals(normalize(status));
    }

    public static boolean canDoctorComplete(String status) {
        return CHECKED_IN.equals(normalize(status));
    }

    public static boolean canDoctorMarkNoShow(String status) {
        return CONFIRMED.equals(normalize(status));
    }

    public static boolean canPatientCancel(String status) {
        String s = normalize(status);
        return PENDING.equals(s) || CONFIRMED.equals(s);
    }
}
