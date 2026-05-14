package com.example.frontend_bookingcare.ui.doctor_directory;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Model hiển thị cho danh bạ bác sĩ (patient-facing). Khác với panel bác sĩ
 * (xem ở {@code ui.doctor_panel}) — chỉ dùng để bệnh nhân duyệt/chọn bác sĩ.
 */
public final class DoctorDetail {

    public static final int[] AVATAR_PALETTE = new int[]{
            com.example.frontend_bookingcare.R.drawable.bg_tile_pink,
            com.example.frontend_bookingcare.R.drawable.bg_tile_blue,
            com.example.frontend_bookingcare.R.drawable.bg_tile_teal,
            com.example.frontend_bookingcare.R.drawable.bg_tile_amber,
            com.example.frontend_bookingcare.R.drawable.bg_tile_purple,
            com.example.frontend_bookingcare.R.drawable.bg_tile_green,
            com.example.frontend_bookingcare.R.drawable.bg_tile_orange,
            com.example.frontend_bookingcare.R.drawable.bg_tile_red,
    };

    /** Danh hiệu/học vị (có thể null): ví dụ "PGS. TS. BS", "ThS. BS", "BS.". */
    @Nullable
    public final String title;
    public final String name;
    @Nullable
    public final Integer yearsExp;
    public final List<String> specialties;
    @Nullable
    public final String address;
    @DrawableRes
    public final int avatarBg;
    /** ID bác sĩ trong DB (có thể null nếu seed data không có). Dùng cho màn chi tiết. */
    @Nullable
    public final Integer doctorId;
    /** Tiểu sử/bio lấy từ backend (DoctorDto.bio). */
    @Nullable
    public final String bio;

    public DoctorDetail(@Nullable String title, String name, @Nullable Integer yearsExp,
                        List<String> specialties, @Nullable String address, @DrawableRes int avatarBg) {
        this(title, name, yearsExp, specialties, address, avatarBg, null, null);
    }

    public DoctorDetail(@Nullable String title, String name, @Nullable Integer yearsExp,
                        List<String> specialties, @Nullable String address, @DrawableRes int avatarBg,
                        @Nullable Integer doctorId, @Nullable String bio) {
        this.title = title;
        this.name = name;
        this.yearsExp = yearsExp;
        this.specialties = specialties != null ? specialties : new ArrayList<>();
        this.address = address;
        this.avatarBg = avatarBg;
        this.doctorId = doctorId;
        this.bio = bio;
    }

    public String lastNameInitial() {
        String[] parts = name == null ? new String[0] : name.trim().split("\\s+");
        String last = parts.length == 0 ? "?" : parts[parts.length - 1];
        return last.isEmpty() ? "?" : last.substring(0, 1).toUpperCase();
    }

    /**
     * Tách học vị khỏi fullName. Seed data thường ở dạng "BS. Nguyễn Văn A",
     * đôi khi "PGS. TS. BS Nguyễn Thị B"...
     */
    public static String[] splitTitleAndName(String fullName) {
        if (fullName == null) return new String[]{null, ""};
        String s = fullName.trim();
        // Danh sách tiền tố phổ biến, sắp xếp dài nhất trước.
        String[] prefixes = new String[]{
                "PGS. TS. BS.", "PGS. TS. BS", "PGS. TS.", "PGS. BS.",
                "TS. BS.", "TS. BS", "ThS. BS.", "ThS. BS",
                "BS. CKII", "BS. CKI", "BS.", "ThS.", "TS.", "PGS."
        };
        for (String p : prefixes) {
            if (s.startsWith(p + " ")) {
                return new String[]{p, s.substring(p.length() + 1).trim()};
            }
            if (s.equalsIgnoreCase(p)) {
                return new String[]{p, ""};
            }
        }
        return new String[]{null, s};
    }

    public static int avatarBgForIndex(int index) {
        int i = ((index % AVATAR_PALETTE.length) + AVATAR_PALETTE.length) % AVATAR_PALETTE.length;
        return AVATAR_PALETTE[i];
    }
}
