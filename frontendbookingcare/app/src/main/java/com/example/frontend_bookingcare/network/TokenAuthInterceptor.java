package com.example.frontend_bookingcare.network;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.frontend_bookingcare.BookingCareApp;
import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.AuthResponse;
import com.example.frontend_bookingcare.api.RefreshRequest;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionExpiredBus;
import com.example.frontend_bookingcare.session.SessionManager;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Interceptor tự refresh access token khi backend trả 401 (Unauthorized).
 *
 * Luồng:
 *  1. Gửi request gốc.
 *  2. Nếu HTTP 401 và request có Authorization → đọc {@code refreshToken} trong
 *     {@link SessionManager}, gọi {@code POST /api/v1/auth/refresh} đồng bộ.
 *  3. Nếu refresh thành công: lưu {@code accessToken}/{@code refreshToken} mới rồi
 *     replay request gốc 1 lần với token mới.
 *  4. Nếu refresh thất bại (refresh token cũng hết hạn / bị revoke): xoá session để
 *     các tab phía sau hiện đúng trạng thái "chưa đăng nhập".
 *
 * Tránh đệ quy:
 *  - Bỏ qua endpoint {@code /api/v1/auth/*} (login/register/refresh).
 *  - Gắn header {@code X-Retry-After-Refresh} cho lần replay → không refresh tiếp.
 *  - Dùng OkHttpClient nội bộ (không qua RetrofitClient) để gọi refresh, tránh
 *    chính interceptor này can thiệp lần nữa.
 */
public class TokenAuthInterceptor implements Interceptor {

    private static final String TAG = "TokenAuthInterceptor";
    private static final String AUTH_HEADER = "Authorization";
    private static final String RETRY_MARK = "X-Retry-After-Refresh";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseUrl;
    private final Gson gson;
    private final OkHttpClient refreshClient;
    /**
     * Khoá tránh hai request song song cùng gọi /refresh — chỉ refresh 1 lần,
     * các request còn lại sẽ tự đọc access token mới đã lưu trong SessionManager.
     */
    private final ReentrantLock refreshLock = new ReentrantLock();

    public TokenAuthInterceptor(String baseUrl, Gson gson) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.gson = gson;
        this.refreshClient = new OkHttpClient();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Response response = chain.proceed(original);

        if (!shouldAttemptRefresh(original, response)) {
            return response;
        }

        String originalAccessToken = stripBearer(original.header(AUTH_HEADER));
        String latestAccess = tryRefreshOnce(originalAccessToken);
        if (latestAccess == null) {
            // Refresh thất bại → trả nguyên response 401 cho caller. Session đã bị clear
            // (nếu refresh token cũng hỏng) để UI biết hiển thị màn login.
            return response;
        }

        response.close();
        Request replay = original.newBuilder()
                .header(AUTH_HEADER, "Bearer " + latestAccess)
                .header(RETRY_MARK, "1")
                .build();
        return chain.proceed(replay);
    }

    private boolean shouldAttemptRefresh(Request request, Response response) {
        int code = response.code();
        if (code != 401) return false;
        if (request.header(AUTH_HEADER) == null) return false;
        if (request.header(RETRY_MARK) != null) return false;
        String path = request.url().encodedPath();
        // Không refresh trên chính các endpoint auth (login/refresh/logout/...).
        return !path.contains("/api/v1/auth/");
    }

    /**
     * Thực hiện refresh đồng bộ. Trả về access token mới hoặc null nếu refresh fail.
     * Chỉ refresh 1 lần kể cả khi nhiều request 401 cùng lúc.
     */
    @Nullable
    private String tryRefreshOnce(@Nullable String requestAccessToken) {
        Context ctx = BookingCareApp.appContext();
        if (ctx == null) return null;
        SessionManager sm = new SessionManager(ctx);

        refreshLock.lock();
        try {
            AuthSession current = sm.getSession();
            if (current == null) return null;

            // Nếu access token đã được request khác refresh xong, dùng luôn token mới
            // mà không phải gọi /refresh lần nữa.
            if (current.accessToken != null
                    && requestAccessToken != null
                    && !current.accessToken.equals(requestAccessToken)) {
                return current.accessToken;
            }

            if (current.refreshToken == null || current.refreshToken.isEmpty()) {
                sm.clearAll();
                SessionExpiredBus.notifyExpired();
                return null;
            }

            AuthResponse refreshed = callRefresh(current.refreshToken);
            if (refreshed == null || refreshed.accessToken == null || refreshed.accessToken.isEmpty()) {
                // Refresh token cũng hết hạn / revoked → xoá session để app điều hướng về login.
                sm.clearAll();
                SessionExpiredBus.notifyExpired();
                return null;
            }

            AuthSession next = new AuthSession(
                    current.userId,
                    refreshed.accessToken,
                    refreshed.refreshToken != null ? refreshed.refreshToken : current.refreshToken,
                    refreshed.role != null ? refreshed.role : current.role,
                    refreshed.fullName != null ? refreshed.fullName : current.fullName,
                    refreshed.email != null ? refreshed.email : current.email
            );
            sm.saveSession(next);
            return next.accessToken;
        } finally {
            refreshLock.unlock();
        }
    }

    @Nullable
    private AuthResponse callRefresh(String refreshToken) {
        try {
            RequestBody body = RequestBody.create(
                    gson.toJson(new RefreshRequest(refreshToken)), JSON);
            Request req = new Request.Builder()
                    .url(baseUrl + "api/v1/auth/refresh")
                    .post(body)
                    .build();
            try (Response resp = refreshClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) return null;
                ResponseBody rb = resp.body();
                if (rb == null) return null;
                ApiEnvelope env = gson.fromJson(rb.string(), ApiEnvelope.class);
                if (env == null || !env.success || env.data == null || env.data.isJsonNull()) {
                    return null;
                }
                return gson.fromJson(env.data, AuthResponse.class);
            }
        } catch (Exception e) {
            Log.w(TAG, "Refresh token call failed: " + e.getMessage());
            return null;
        }
    }

    @Nullable
    private static String stripBearer(@Nullable String header) {
        if (header == null) return null;
        if (header.startsWith("Bearer ")) return header.substring(7).trim();
        return header.trim();
    }
}
