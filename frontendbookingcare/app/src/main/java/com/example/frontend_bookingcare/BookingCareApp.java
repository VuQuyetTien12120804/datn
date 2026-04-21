package com.example.frontend_bookingcare;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.frontend_bookingcare.locale.LocaleStore;

public class BookingCareApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Thiết kế UI hiện tại dùng nền sáng; ép app chạy light mode để tránh dark mode
        // làm chữ/màu bị đảo ngược, trông "không đồng bộ".
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        LocaleStore.applySaved(this);
    }
}
