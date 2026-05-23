package com.example.frontend_bookingcare.ui.doctor_panel;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.example.frontend_bookingcare.R;

final class DoctorEmptyUi {

    private DoctorEmptyUi() {
    }

    static void bind(@NonNull View emptyRoot, @DrawableRes int iconRes, @StringRes int titleRes, @StringRes int hintRes) {
        ImageView icon = emptyRoot.findViewById(R.id.doctor_empty_icon);
        TextView title = emptyRoot.findViewById(R.id.doctor_empty_title);
        TextView hint = emptyRoot.findViewById(R.id.doctor_empty_hint);
        if (icon != null) icon.setImageResource(iconRes);
        if (title != null) title.setText(titleRes);
        if (hint != null) hint.setText(hintRes);
    }

    static void bindHero(@NonNull View root, @DrawableRes int iconRes,
                         @StringRes int titleRes, @StringRes int subtitleRes) {
        ImageView icon = root.findViewById(R.id.doctor_hero_icon);
        TextView title = root.findViewById(R.id.doctor_hero_title);
        TextView subtitle = root.findViewById(R.id.doctor_hero_subtitle);
        if (icon != null) icon.setImageResource(iconRes);
        if (title != null) title.setText(titleRes);
        if (subtitle != null) subtitle.setText(subtitleRes);
    }
}
