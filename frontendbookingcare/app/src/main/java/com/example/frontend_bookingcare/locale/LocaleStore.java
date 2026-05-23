package com.example.frontend_bookingcare.locale;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Lưu lựa chọn ngôn ngữ (vi/en) và áp dụng qua per-app locale của AppCompat.
 */
public final class LocaleStore {

    private static final String PREFS = "bookingcare_app_settings";
    private static final String KEY_LOCALE = "app_locale_tag";

    private LocaleStore() {
    }

    public static String getSavedTag(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LOCALE, "vi");
    }

    public static boolean isEnglish(Context context) {
        return "en".equals(getSavedTag(context));
    }

    public static void saveAndApply(Context context, String languageTag) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LOCALE, languageTag)
                .apply();
        AppCompatDelegate.setApplicationLocales(localeListForTag(languageTag));
    }

    public static void applySaved(Context context) {
        AppCompatDelegate.setApplicationLocales(localeListForTag(getSavedTag(context)));
    }

    /**
     * Keep Vietnamese in the locale list even when UI is English so IME can compose
     * Vietnamese text (Telex/VNI) in chat, profile, search, etc.
     */
    private static LocaleListCompat localeListForTag(String languageTag) {
        if ("en".equals(languageTag)) {
            return LocaleListCompat.forLanguageTags("en,vi");
        }
        return LocaleListCompat.forLanguageTags("vi,en");
    }
}
