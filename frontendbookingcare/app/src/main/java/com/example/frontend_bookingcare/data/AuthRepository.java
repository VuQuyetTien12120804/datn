package com.example.frontend_bookingcare.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.account.RegisterDraft;
import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.AuthApiService;
import com.example.frontend_bookingcare.api.AuthRequest;
import com.example.frontend_bookingcare.api.AuthResponse;
import com.example.frontend_bookingcare.api.ChangePasswordRequest;
import com.example.frontend_bookingcare.api.EmailOtpResponse;
import com.example.frontend_bookingcare.api.LogoutRequest;
import com.example.frontend_bookingcare.api.PasswordResetConfirmRequest;
import com.example.frontend_bookingcare.api.RegisterRequest;
import com.example.frontend_bookingcare.api.RequestEmailOtpRequest;
import com.example.frontend_bookingcare.api.RetrofitClient;
import com.example.frontend_bookingcare.api.VerifyEmailOtpRequest;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class AuthRepository {

    public interface ResultCallback<T> {
        void onDone(T data, String errorMessage);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final AuthApiService api = RetrofitClient.authApi();
    private final Gson gson = RetrofitClient.gson();
    private final SessionManager sessionManager;
    private final Context appCtx;

    public AuthRepository(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.appCtx = sessionManager.getAppContext();
    }

    private void postMain(Runnable r) {
        MAIN.post(r);
    }

    private AuthSession toSession(AuthResponse dto) {
        String uid = dto.userId != null && !dto.userId.isEmpty() ? dto.userId : dto.email;
        return new AuthSession(
                uid,
                dto.accessToken,
                dto.refreshToken,
                dto.role != null ? dto.role : "",
                dto.fullName != null ? dto.fullName : "",
                dto.email != null ? dto.email : ""
        );
    }

    private AuthResponse parseAuthData(JsonElement data) {
        if (data == null || data.isJsonNull()) {
            throw new IllegalStateException(appCtx.getString(R.string.error_missing_server_data));
        }
        return gson.fromJson(data, AuthResponse.class);
    }

    private String readHttpError(retrofit2.Response<?> response) {
        okhttp3.ResponseBody eb = response.errorBody();
        if (eb != null) {
            try {
                String raw = eb.string();
                ApiEnvelope env = gson.fromJson(raw, ApiEnvelope.class);
                if (env != null && env.message != null && !env.message.isEmpty()) {
                    return env.message;
                }
                JsonObject obj = gson.fromJson(raw, JsonObject.class);
                if (obj != null && obj.has("message") && !obj.get("message").isJsonNull()) {
                    String m = obj.get("message").getAsString();
                    if (m != null && !m.isEmpty()) {
                        return m;
                    }
                }
            } catch (IOException ignored) {
            } catch (Exception ignored) {
            }
        }
        return appCtx.getString(R.string.error_network_fmt, response.code());
    }

    public void login(String email, String password, ResultCallback<AuthSession> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.login(new AuthRequest(email.trim(), password)).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success && body.data != null && !body.data.isJsonNull()) {
                    AuthResponse dto = parseAuthData(body.data);
                    AuthSession session = toSession(dto);
                    sessionManager.saveSession(session);
                    postMain(() -> cb.onDone(session, null));
                } else {
                    String msg = body != null && !body.success ? body.message : readHttpError(response);
                    postMain(() -> cb.onDone(null, msg));
                }
            } catch (Exception e) {
                postMain(() -> cb.onDone(null, e.getMessage() != null ? e.getMessage() : appCtx.getString(R.string.error_login_failed)));
            }
        });
    }

    public void requestRegisterOtp(String email, ResultCallback<EmailOtpResponse> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.requestEmailOtp(new RequestEmailOtpRequest(email.trim())).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    EmailOtpResponse otp = null;
                    if (body.data != null && !body.data.isJsonNull()) {
                        otp = gson.fromJson(body.data, EmailOtpResponse.class);
                    }
                    EmailOtpResponse finalOtp = otp;
                    postMain(() -> cb.onDone(finalOtp, null));
                } else {
                    String msg = body != null && !body.success ? body.message : readHttpError(response);
                    postMain(() -> cb.onDone(null, msg));
                }
            } catch (Exception e) {
                postMain(() -> cb.onDone(null, e.getMessage() != null ? e.getMessage() : appCtx.getString(R.string.error_otp_send_failed)));
            }
        });
    }

    public void requestPasswordResetOtp(String email, ResultCallback<EmailOtpResponse> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.requestPasswordResetOtp(new RequestEmailOtpRequest(email.trim())).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    EmailOtpResponse otp = null;
                    if (body.data != null && !body.data.isJsonNull()) {
                        otp = gson.fromJson(body.data, EmailOtpResponse.class);
                    }
                    EmailOtpResponse finalOtp = otp;
                    postMain(() -> cb.onDone(finalOtp, null));
                } else {
                    String msg = body != null && !body.success ? body.message : readHttpError(response);
                    postMain(() -> cb.onDone(null, msg));
                }
            } catch (Exception e) {
                postMain(() -> cb.onDone(null, e.getMessage() != null ? e.getMessage() : appCtx.getString(R.string.error_otp_send_failed)));
            }
        });
    }

    public void confirmPasswordReset(String email, String otp, String newPassword, ResultCallback<Void> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.confirmPasswordReset(
                        new PasswordResetConfirmRequest(email.trim(), otp.trim(), newPassword)
                ).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    postMain(() -> cb.onDone(null, null));
                } else {
                    String msg = body != null && !body.success ? body.message : readHttpError(response);
                    postMain(() -> cb.onDone(null, msg));
                }
            } catch (Exception e) {
                postMain(() -> cb.onDone(null, e.getMessage() != null ? e.getMessage() : appCtx.getString(R.string.error_password_reset_failed)));
            }
        });
    }

    public void verifyRegisterOtp(String email, String otp, ResultCallback<Void> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.verifyEmailOtp(new VerifyEmailOtpRequest(email.trim(), otp.trim())).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    postMain(() -> cb.onDone(null, null));
                } else {
                    String msg = body != null && !body.success ? body.message : readHttpError(response);
                    postMain(() -> cb.onDone(null, msg));
                }
            } catch (Exception e) {
                postMain(() -> cb.onDone(null, e.getMessage() != null ? e.getMessage() : appCtx.getString(R.string.error_verify_failed)));
            }
        });
    }

    public void register(RegisterDraft draft, ResultCallback<AuthSession> cb) {
        EXECUTOR.execute(() -> {
            try {
                RegisterRequest req = new RegisterRequest();
                req.email = draft.email.trim();
                req.password = draft.password;
                req.fullName = draft.fullName.trim();
                req.phone = draft.phone != null && !draft.phone.trim().isEmpty() ? draft.phone.trim() : null;
                Response<ApiEnvelope> response = api.register(req).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success && body.data != null && !body.data.isJsonNull()) {
                    AuthResponse dto = parseAuthData(body.data);
                    AuthSession session = toSession(dto);
                    sessionManager.saveSession(session);
                    postMain(() -> cb.onDone(session, null));
                } else {
                    String msg = body != null && !body.success ? body.message : readHttpError(response);
                    postMain(() -> cb.onDone(null, msg));
                }
            } catch (Exception e) {
                postMain(() -> cb.onDone(null, e.getMessage() != null ? e.getMessage() : appCtx.getString(R.string.error_register_failed)));
            }
        });
    }

    public void verifyOtpThenRegister(String email, String otp, RegisterDraft draft, ResultCallback<AuthSession> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> v = api.verifyEmailOtp(new VerifyEmailOtpRequest(email.trim(), otp.trim())).execute();
                ApiEnvelope vBody = v.body();
                if (!v.isSuccessful() || vBody == null || !vBody.success) {
                    String msg = vBody != null && !vBody.success ? vBody.message : readHttpError(v);
                    postMain(() -> cb.onDone(null, msg));
                    return;
                }
                RegisterRequest req = new RegisterRequest();
                req.email = draft.email.trim();
                req.password = draft.password;
                req.fullName = draft.fullName.trim();
                req.phone = draft.phone != null && !draft.phone.trim().isEmpty() ? draft.phone.trim() : null;
                Response<ApiEnvelope> r = api.register(req).execute();
                ApiEnvelope body = r.body();
                if (r.isSuccessful() && body != null && body.success && body.data != null && !body.data.isJsonNull()) {
                    AuthResponse dto = parseAuthData(body.data);
                    AuthSession session = toSession(dto);
                    sessionManager.saveSession(session);
                    postMain(() -> cb.onDone(session, null));
                } else {
                    String msg = body != null && !body.success ? body.message : readHttpError(r);
                    postMain(() -> cb.onDone(null, msg));
                }
            } catch (Exception e) {
                postMain(() -> cb.onDone(null, e.getMessage() != null ? e.getMessage() : appCtx.getString(R.string.error_register_failed)));
            }
        });
    }

    public void changePassword(String oldPassword, String newPassword, ResultCallback<Void> cb) {
        EXECUTOR.execute(() -> {
            try {
                AuthSession s = sessionManager.getSession();
                if (s == null) {
                    postMain(() -> cb.onDone(null, appCtx.getString(R.string.error_not_logged_in)));
                    return;
                }
                Response<ApiEnvelope> response = api.changePassword(
                        "Bearer " + s.accessToken,
                        new ChangePasswordRequest(oldPassword, newPassword)
                ).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    postMain(() -> cb.onDone(null, null));
                } else {
                    String msg = body != null && !body.success ? body.message : readHttpError(response);
                    postMain(() -> cb.onDone(null, msg));
                }
            } catch (Exception e) {
                postMain(() -> cb.onDone(null, e.getMessage() != null ? e.getMessage() : appCtx.getString(R.string.error_change_password_failed)));
            }
        });
    }

    public void logout(ResultCallback<Void> cb) {
        EXECUTOR.execute(() -> {
            try {
                AuthSession s = sessionManager.getSession();
                if (s != null) {
                    try {
                        api.logout("Bearer " + s.accessToken, new LogoutRequest(s.refreshToken)).execute();
                    } catch (Exception ignored) {
                    }
                }
                sessionManager.clearAll();
                postMain(() -> cb.onDone(null, null));
            } catch (Exception e) {
                sessionManager.clearAll();
                postMain(() -> cb.onDone(null, null));
            }
        });
    }
}
