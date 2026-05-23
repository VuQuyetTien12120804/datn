package com.example.frontend_bookingcare.ui.doctor_panel;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.onboarding.OnboardingActivity;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;

/**
 * Chặn truy cập {@link DoctorMainActivity} khi chưa đăng nhập hoặc không phải role bác sĩ.
 */
public final class DoctorSessionGuard {

    private DoctorSessionGuard() {
    }

    /** @return {@code false} nếu đã redirect sang màn đăng nhập. */
    public static boolean ensureDoctorSession(@NonNull AppCompatActivity activity) {
        SessionManager sm = new SessionManager(activity);
        AuthSession session = sm.getSession();
        if (sm.isLoggedIn() && session != null && "doctor".equalsIgnoreCase(session.role)) {
            return true;
        }
        Toast.makeText(activity, R.string.doctor_panel_session_required, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra(OnboardingActivity.EXTRA_OPEN_ACCOUNT, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return false;
    }
}
