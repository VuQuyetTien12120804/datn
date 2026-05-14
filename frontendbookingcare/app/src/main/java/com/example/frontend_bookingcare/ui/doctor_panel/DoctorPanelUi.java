package com.example.frontend_bookingcare.ui.doctor_panel;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.frontend_bookingcare.R;

/**
 * Avatar chữ cái & màu nền cho danh sách bệnh nhân trong panel bác sĩ.
 */
public final class DoctorPanelUi {

    private static final int[] PATIENT_AVATAR_COLORS = {
            R.color.doctor_brand_primary,
            R.color.home_tile_blue,
            R.color.home_tile_teal,
            R.color.home_tile_purple,
            R.color.home_tile_orange,
            R.color.teal_primary,
            R.color.home_tile_pink,
    };

    private DoctorPanelUi() {
    }

    public static String initials(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) return "?";
        String n = displayName.trim();
        if ("—".equals(n)) return "?";
        String[] parts = n.split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            if (p.length() >= 2) {
                return p.substring(0, 2).toUpperCase(java.util.Locale.ROOT);
            }
            return p.toUpperCase(java.util.Locale.ROOT);
        }
        String a = parts[0].isEmpty() ? "" : parts[0].substring(0, 1);
        String b = parts[parts.length - 1].isEmpty() ? "" : parts[parts.length - 1].substring(0, 1);
        return (a + b).toUpperCase(java.util.Locale.ROOT);
    }

    /** TextView hiển thị chữ cái, nền tròn màu theo tên. */
    public static void stylePatientAvatar(TextView tv, String patientName, Context ctx) {
        tv.setText(initials(patientName));
        tv.setTextColor(0xFFFFFFFF);
        int idx = Math.abs((patientName != null ? patientName : "").hashCode()) % PATIENT_AVATAR_COLORS.length;
        int color = ContextCompat.getColor(ctx, PATIENT_AVATAR_COLORS[idx]);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        tv.setBackground(g);
    }
}
