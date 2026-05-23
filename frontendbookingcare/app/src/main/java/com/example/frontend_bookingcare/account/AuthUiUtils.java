package com.example.frontend_bookingcare.account;

import androidx.annotation.Nullable;

import java.util.Locale;

/** Gợi ý UI dựa trên thông báo lỗi từ API auth. */
public final class AuthUiUtils {

    private AuthUiUtils() {
    }

    /**
     * Backend: {@code ErrorCode.EMAIL_EXISTS} → "Email đã được đăng ký".
     */
    public static boolean isEmailAlreadyRegisteredMessage(@Nullable String message) {
        if (message == null || message.isEmpty()) return false;
        String m = message.toLowerCase(Locale.ROOT);
        return m.contains("email đã được đăng ký")
                || m.contains("email da duoc dang ky")
                || m.contains("email already registered")
                || m.contains("email is already registered");
    }
}
