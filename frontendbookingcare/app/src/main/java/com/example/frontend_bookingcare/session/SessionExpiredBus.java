package com.example.frontend_bookingcare.session;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Thông báo UI khi {@link SessionManager#clearAll()} do refresh token hết hạn. */
public final class SessionExpiredBus {

    public interface Listener {
        void onSessionExpired();
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private SessionExpiredBus() {
    }

    public static void register(Listener listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    public static void unregister(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static void notifyExpired() {
        MAIN.post(() -> {
            for (Listener l : LISTENERS) {
                l.onSessionExpired();
            }
        });
    }
}
