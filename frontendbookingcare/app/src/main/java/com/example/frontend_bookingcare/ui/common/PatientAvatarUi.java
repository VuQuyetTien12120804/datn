package com.example.frontend_bookingcare.ui.common;

import android.content.Context;
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
        tv.setTextColor(0xFFFFFFFF);
        int idx = Math.abs((displayName != null ? displayName : "").hashCode()) % AVATAR_COLORS.length;
        int color = ContextCompat.getColor(ctx, AVATAR_COLORS[idx]);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        tv.setBackground(g);
    }
}
