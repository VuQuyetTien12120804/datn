package com.example.frontend_bookingcare;

import android.app.Application;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.frontend_bookingcare.locale.LocaleStore;

public class BookingCareApp extends Application {

    @Nullable
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        // Thiết kế UI hiện tại dùng nền sáng; ép app chạy light mode để tránh dark mode
        // làm chữ/màu bị đảo ngược, trông "không đồng bộ".
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        LocaleStore.applySaved(this);
    }

    /**
     * Cho phép các thành phần singleton không có Context (vd. OkHttp interceptor)
     * truy cập ApplicationContext để đọc/ghi SessionManager.
     */
    @Nullable
    public static Context appContext() {
        return appContext;
    }
}
