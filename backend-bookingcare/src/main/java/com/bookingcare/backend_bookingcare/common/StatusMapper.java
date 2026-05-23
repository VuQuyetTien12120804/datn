package com.bookingcare.backend_bookingcare.common;

public final class StatusMapper {

    private StatusMapper() {
    }

    public static String toApi(String dbStatus) {
        if (dbStatus == null || dbStatus.isBlank()) {
            return "PENDING";
        }
        return dbStatus.trim().toUpperCase();
    }

    public static String toDb(String apiStatus) {
        if (apiStatus == null || apiStatus.isBlank()) {
            return "pending";
        }
        return apiStatus.trim().toLowerCase();
    }
}
