package com.example.frontend_bookingcare.session;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.example.frontend_bookingcare.ui.doctor_panel.DoctorMainActivity;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Quy định role nào thì vào Activity nào sau đăng nhập.
 *
 * <p>Khi dự án thêm role mới (ví dụ {@code admin}), chỉ cần bổ sung vào bảng
 * {@link #ROUTES} thay vì đi sửa {@code if/else} ở nhiều Activity/Fragment.
 * Role không có trong bảng (null, {@code "patient"}, role lạ) được coi là
 * mặc định — giữ nguyên {@code MainActivity} hiện tại.
 */
public final class RoleRouter {

    private static final Map<String, Class<? extends Activity>> ROUTES = new LinkedHashMap<>();

    static {
        ROUTES.put("doctor", DoctorMainActivity.class);
        // ROUTES.put("admin", AdminMainActivity.class); // ví dụ khi có admin panel
    }

    private RoleRouter() {
    }

    /**
     * Trả về Activity tương ứng với {@code role}, hoặc {@code null} nếu role rơi
     * vào luồng mặc định (patient / guest).
     */
    @Nullable
    public static Class<? extends Activity> activityFor(@Nullable String role) {
        if (role == null) return null;
        return ROUTES.get(role.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Nếu {@code session} có role cần chuyển Activity riêng, tạo {@link Intent}
     * tương ứng kèm flag {@code CLEAR_TOP}. Trả về {@code null} nếu không cần
     * chuyển (ví dụ role {@code patient} hoặc chưa đăng nhập).
     */
    @Nullable
    public static Intent intentFor(Context ctx, @Nullable AuthSession session) {
        if (session == null) return null;
        Class<? extends Activity> target = activityFor(session.role);
        if (target == null) return null;
        Intent i = new Intent(ctx, target);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }
}
