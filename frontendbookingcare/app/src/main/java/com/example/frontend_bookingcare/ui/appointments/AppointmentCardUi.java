package com.example.frontend_bookingcare.ui.appointments;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.ui.common.PatientAvatarUi;

import java.util.Locale;

/** Màu chip trạng thái + avatar cho thẻ lịch khám. */
public final class AppointmentCardUi {

    private AppointmentCardUi() {
    }

    public static void bindDoctorAvatar(@NonNull TextView avatar, @Nullable String doctorName, @NonNull Context ctx) {
        String letter = firstLetter(doctorName);
        avatar.setText(letter);
        PatientAvatarUi.styleLetterAvatar3D(avatar, doctorName != null ? doctorName : "?", ctx);
    }

    public static void bindStatus(@NonNull TextView statusChip, @Nullable View accentBar,
                                  @Nullable String status, @NonNull Context ctx) {
        StatusStyle style = styleFor(status);
        int bg = ContextCompat.getColor(ctx, style.chipBg);
        int fg = ContextCompat.getColor(ctx, style.chipFg);
        GradientDrawable chip = new GradientDrawable();
        chip.setShape(GradientDrawable.RECTANGLE);
        chip.setCornerRadius(999f);
        chip.setColor(bg);
        statusChip.setBackground(chip);
        statusChip.setTextColor(fg);
        if (accentBar != null) {
            accentBar.setBackgroundColor(ContextCompat.getColor(ctx, style.accent));
        }
    }

    private static StatusStyle styleFor(@Nullable String status) {
        String s = status != null ? status.trim().toLowerCase(Locale.ROOT) : "";
        return switch (s) {
            case "confirmed" -> new StatusStyle(R.color.icon_bg_blue, R.color.brand_primary, R.color.brand_primary);
            case "checked_in" -> new StatusStyle(R.color.icon_bg_teal, R.color.teal_primary, R.color.teal_primary);
            case "completed" -> new StatusStyle(R.color.doctor_status_done_bg, R.color.doctor_status_done_fg,
                    R.color.doctor_status_done_fg);
            case "cancelled" -> new StatusStyle(R.color.icon_bg_grey, R.color.text_secondary_dim, R.color.text_muted);
            case "no_show" -> new StatusStyle(R.color.icon_bg_red, R.color.doctor_logout_red, R.color.doctor_logout_red);
            default -> new StatusStyle(R.color.icon_bg_yellow, R.color.home_tile_amber, R.color.home_tile_amber);
        };
    }

    private static String firstLetter(@Nullable String name) {
        if (name == null) return "?";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "?";
        String[] parts = trimmed.split("\\s+");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? "?" : last.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private record StatusStyle(@ColorRes int chipBg, @ColorRes int chipFg, @ColorRes int accent) {
    }
}
