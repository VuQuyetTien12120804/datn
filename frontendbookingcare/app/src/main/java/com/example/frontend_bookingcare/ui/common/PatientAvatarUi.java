package com.example.frontend_bookingcare.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.frontend_bookingcare.R;

/** Avatar chữ cái cho luồng bệnh nhân (tin nhắn, v.v.). */
public final class PatientAvatarUi {

    private static final int[] AVATAR_COLORS = {
            R.color.doctor_brand_primary,
            R.color.home_tile_blue,
            R.color.home_tile_teal,
            R.color.home_tile_purple,
            R.color.home_tile_orange,
            R.color.teal_primary,
            R.color.home_tile_pink,
    };

    private PatientAvatarUi() {
    }

    public static void styleLetterAvatar(TextView tv, String displayName, Context ctx) {
        applyGradientAvatar(tv, displayName, ctx, false);
    }

    /** Avatar chữ cái kiểu 3D (gradient + viền sáng) — dùng danh sách tin nhắn. */
    public static void styleLetterAvatar3D(TextView tv, String displayName, Context ctx) {
        applyGradientAvatar(tv, displayName, ctx, true);
    }

    private static void applyGradientAvatar(TextView tv, String displayName, Context ctx, boolean depth3d) {
        tv.setTextColor(0xFFFFFFFF);
        int idx = Math.abs((displayName != null ? displayName : "").hashCode()) % AVATAR_COLORS.length;
        int color = ContextCompat.getColor(ctx, AVATAR_COLORS[idx]);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        if (depth3d) {
            int light = blend(color, 0xFFFFFFFF, 0.28f);
            int dark = blend(color, 0xFF000000, 0.22f);
            g.setOrientation(GradientDrawable.Orientation.TL_BR);
            g.setColors(new int[]{light, color, dark});
            g.setStroke((int) (2 * ctx.getResources().getDisplayMetrics().density), 0x55FFFFFF);
            tv.setElevation(6f);
        } else {
            g.setColor(color);
            tv.setElevation(0f);
        }
        tv.setBackground(g);
    }

    private static int blend(int base, int overlay, float ratio) {
        int r = (int) (Color.red(base) * (1f - ratio) + Color.red(overlay) * ratio);
        int g = (int) (Color.green(base) * (1f - ratio) + Color.green(overlay) * ratio);
        int b = (int) (Color.blue(base) * (1f - ratio) + Color.blue(overlay) * ratio);
        return Color.rgb(clamp(r), clamp(g), clamp(b));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
