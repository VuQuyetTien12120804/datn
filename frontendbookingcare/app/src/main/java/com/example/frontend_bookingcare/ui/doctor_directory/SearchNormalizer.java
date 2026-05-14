package com.example.frontend_bookingcare.ui.doctor_directory;

import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Chuẩn hoá chuỗi tiếng Việt cho tìm kiếm: bỏ dấu + lowercase.
 * Ví dụ: "Nguyễn Thị Lan" → "nguyen thi lan", giúp user gõ "lan" hoặc "hong"
 * (không dấu) vẫn match với "Nguyễn Thị Hồng".
 *
 * Thêm 1 lớp nhỏ để tránh rải logic normalize khắp nơi và dễ unit test về sau.
 */
public final class SearchNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private SearchNormalizer() {
    }

    public static String normalize(@Nullable String input) {
        if (input == null) return "";
        String noAccent = Normalizer.normalize(input, Normalizer.Form.NFD);
        noAccent = DIACRITICS.matcher(noAccent).replaceAll("");
        // Chữ đ/Đ không bị Normalizer tách dấu → thay thủ công.
        noAccent = noAccent.replace('đ', 'd').replace('Đ', 'D');
        return noAccent.toLowerCase().trim();
    }
}
