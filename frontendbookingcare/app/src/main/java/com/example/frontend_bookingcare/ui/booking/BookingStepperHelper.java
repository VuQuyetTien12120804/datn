package com.example.frontend_bookingcare.ui.booking;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.frontend_bookingcare.R;

/**
 * Set trạng thái stepper (1/2/3) cho layout {@code include_booking_stepper.xml}.
 *
 * Quy ước:
 *  - "active"   = bước đang đứng → nền brand_primary, label xanh đậm
 *  - "done"     = bước đã hoàn tất → nền xanh lá
 *  - "inactive" = bước chưa tới → nền xám
 */
public final class BookingStepperHelper {

    private BookingStepperHelper() {
    }

    public static void bind(View stepperRoot, int activeStep) {
        TextView d1 = stepperRoot.findViewById(R.id.booking_stepper_dot_1);
        TextView d2 = stepperRoot.findViewById(R.id.booking_stepper_dot_2);
        TextView d3 = stepperRoot.findViewById(R.id.booking_stepper_dot_3);
        TextView l1 = stepperRoot.findViewById(R.id.booking_stepper_label_1);
        TextView l2 = stepperRoot.findViewById(R.id.booking_stepper_label_2);
        TextView l3 = stepperRoot.findViewById(R.id.booking_stepper_label_3);

        apply(d1, l1, 1, activeStep, stepperRoot);
        apply(d2, l2, 2, activeStep, stepperRoot);
        apply(d3, l3, 3, activeStep, stepperRoot);
    }

    private static void apply(TextView dot, TextView label, int stepIndex, int activeStep, View root) {
        if (stepIndex < activeStep) {
            dot.setBackgroundResource(R.drawable.bg_booking_step_done);
            label.setTextColor(ContextCompat.getColor(root.getContext(), R.color.doctor_status_done_fg));
            label.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (stepIndex == activeStep) {
            dot.setBackgroundResource(R.drawable.bg_booking_step_active);
            label.setTextColor(ContextCompat.getColor(root.getContext(), R.color.brand_primary));
            label.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            dot.setBackgroundResource(R.drawable.bg_booking_step_inactive);
            label.setTextColor(ContextCompat.getColor(root.getContext(), R.color.text_secondary_dim));
            label.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }
}
