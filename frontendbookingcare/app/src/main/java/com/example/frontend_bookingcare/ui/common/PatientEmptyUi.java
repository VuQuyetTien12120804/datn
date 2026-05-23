package com.example.frontend_bookingcare.ui.common;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.example.frontend_bookingcare.R;

public final class PatientEmptyUi {

    private PatientEmptyUi() {
    }

    public static void bind(View root, @DrawableRes int iconRes, @StringRes int titleRes, @StringRes int hintRes) {
        ImageView icon = root.findViewById(R.id.patient_empty_icon);
        TextView title = root.findViewById(R.id.patient_empty_title);
        TextView hint = root.findViewById(R.id.patient_empty_hint);
        if (icon != null) icon.setImageResource(iconRes);
        if (title != null) title.setText(titleRes);
        if (hint != null) hint.setText(hintRes);
    }

    public static void bindMessage(View root, @StringRes int titleRes, CharSequence hintText) {
        ImageView icon = root.findViewById(R.id.patient_empty_icon);
        TextView title = root.findViewById(R.id.patient_empty_title);
        TextView hint = root.findViewById(R.id.patient_empty_hint);
        if (icon != null) icon.setImageResource(android.R.drawable.ic_dialog_info);
        if (title != null) title.setText(titleRes);
        if (hint != null) hint.setText(hintText);
    }
}
