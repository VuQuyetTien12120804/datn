package com.example.frontend_bookingcare.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.frontend_bookingcare.api.RetrofitClient;
import com.google.gson.Gson;

public class SessionManager {

    private static final String PREFS = "bookingcare_prefs";
    private static final String KEY_AUTH = "auth_session_json";
    private static final String KEY_PROFILE_EXTRA = "profile_extras_json";

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Gson gson = RetrofitClient.gson();

    public SessionManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Context getAppContext() {
        return appContext;
    }

    public boolean isLoggedIn() {
        AuthSession s = getSession();
        return s != null && !TextUtils.isEmpty(s.accessToken);
    }

    public AuthSession getSession() {
        String json = prefs.getString(KEY_AUTH, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, AuthSession.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveSession(AuthSession session) {
        prefs.edit().putString(KEY_AUTH, gson.toJson(session)).apply();
    }

    public void clearSession() {
        prefs.edit().remove(KEY_AUTH).apply();
    }

    public ProfileExtras getProfileExtras() {
        String json = prefs.getString(KEY_PROFILE_EXTRA, null);
        if (json == null) return new ProfileExtras();
        try {
            ProfileExtras p = gson.fromJson(json, ProfileExtras.class);
            return p != null ? p : new ProfileExtras();
        } catch (Exception e) {
            return new ProfileExtras();
        }
    }

    public void saveProfileExtras(ProfileExtras extras) {
        prefs.edit().putString(KEY_PROFILE_EXTRA, gson.toJson(extras)).apply();
    }

    public void clearProfileExtras() {
        prefs.edit().remove(KEY_PROFILE_EXTRA).apply();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
