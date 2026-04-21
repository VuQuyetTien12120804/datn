package com.example.frontend_bookingcare.ui.home;

import java.util.HashMap;
import java.util.Map;

/**
 * Map code chuyên khoa (từ DB) → emoji icon hiển thị trên UI.
 * Icon là asset app, không lưu DB. Thêm code mới ở đây khi backend có thêm khoa.
 */
public final class SpecialtyIcons {

    private static final Map<String, String> BY_CODE = new HashMap<>();
    private static final Map<String, String> BY_NAME_NORMALIZED = new HashMap<>();
    private static final String DEFAULT_ICON = "🏥";

    static {
        // Seed backend hiện có: INT, PED, ENT, DERM, CARD, OBGYN, EYE, DENT, ORTHO, GI, ENDO, NEURO
        BY_CODE.put("INT", "🩺");       // Nội tổng quát
        BY_CODE.put("PED", "👶");       // Nhi khoa
        BY_CODE.put("ENT", "👂");       // Tai Mũi Họng
        BY_CODE.put("DERM", "🧴");      // Da liễu
        BY_CODE.put("CARD", "❤️");      // Tim mạch
        BY_CODE.put("OBGYN", "🤰");     // Sản - Phụ khoa
        BY_CODE.put("EYE", "👁️");       // Mắt
        BY_CODE.put("DENT", "🦷");      // Răng - Hàm - Mặt
        BY_CODE.put("ORTHO", "🦴");     // Cơ - Xương - Khớp
        BY_CODE.put("GI", "🫀");        // Tiêu hóa (dạ dày/ruột)
        BY_CODE.put("ENDO", "🧬");      // Nội tiết
        BY_CODE.put("NEURO", "🧠");     // Thần kinh
        // Các khoa thường gặp khác (nếu backend mở rộng sau này)
        BY_CODE.put("PULMO", "🫁");     // Hô hấp / phổi
        BY_CODE.put("URO", "♂️");       // Nam khoa / tiết niệu
        BY_CODE.put("ONCO", "🎗️");      // Ung bướu
        BY_CODE.put("PSY", "🧘");       // Tâm thần
        BY_CODE.put("NUTR", "🥗");      // Dinh dưỡng
        BY_CODE.put("SPORT", "🏃");     // Y học thể thao
        BY_CODE.put("ALLERGY", "🛡️");  // Dị ứng - miễn dịch
        BY_CODE.put("TCM", "☯️");       // Y học cổ truyền

        // Fallback theo tên tiếng Việt đã chuẩn hóa (không dấu, viết thường)
        BY_NAME_NORMALIZED.put("noi tong quat", "🩺");
        BY_NAME_NORMALIZED.put("nhi khoa", "👶");
        BY_NAME_NORMALIZED.put("tai mui hong", "👂");
        BY_NAME_NORMALIZED.put("da lieu", "🧴");
        BY_NAME_NORMALIZED.put("tim mach", "❤️");
        BY_NAME_NORMALIZED.put("san phu khoa", "🤰");
        BY_NAME_NORMALIZED.put("san - phu khoa", "🤰");
        BY_NAME_NORMALIZED.put("mat", "👁️");
        BY_NAME_NORMALIZED.put("rang ham mat", "🦷");
        BY_NAME_NORMALIZED.put("rang - ham - mat", "🦷");
        BY_NAME_NORMALIZED.put("co xuong khop", "🦴");
        BY_NAME_NORMALIZED.put("co - xuong - khop", "🦴");
        BY_NAME_NORMALIZED.put("tieu hoa", "🫀");
        BY_NAME_NORMALIZED.put("noi tiet", "🧬");
        BY_NAME_NORMALIZED.put("than kinh", "🧠");
    }

    private SpecialtyIcons() {
    }

    public static String iconFor(String code, String name) {
        if (code != null) {
            String icon = BY_CODE.get(code.trim().toUpperCase());
            if (icon != null) return icon;
        }
        if (name != null) {
            String key = normalize(name);
            String icon = BY_NAME_NORMALIZED.get(key);
            if (icon != null) return icon;
        }
        return DEFAULT_ICON;
    }

    private static String normalize(String s) {
        String n = java.text.Normalizer.normalize(s.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[đ]", "d");
        return n;
    }
}
