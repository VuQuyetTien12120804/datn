package com.bookingcare.backend_bookingcare.util;

import java.util.Locale;

public final class GenderUtil {

    private GenderUtil() {
    }

    /** Chuẩn hóa giới tính: MALE/FEMALE/OTHER, Nam/Nữ, male/female/other → male|female|other|unknown */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "male", "m", "nam" -> "male";
            case "female", "f", "nu", "nữ" -> "female";
            case "other", "khac", "khác" -> "other";
            default -> s;
        };
    }
}
