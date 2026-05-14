package com.example.frontend_bookingcare.data;

import androidx.annotation.Nullable;

import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.PatientProfileResponseDto;
import com.example.frontend_bookingcare.api.RetrofitClient;
import com.example.frontend_bookingcare.api.UpdatePatientProfileRequest;
import com.example.frontend_bookingcare.session.ProfileExtras;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PatientProfileRepository {

    public interface RepoCallback {
        void onSuccess(ApiEnvelope env);

        void onError(String message);
    }

    public interface PatientIdCallback {
        void onDone(Integer patientId, @Nullable String errorMessage);
    }

    public interface ProfileExtrasCallback {
        void onDone(@Nullable ProfileExtras extras, @Nullable String errorMessage);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public void updateMe(String bearerToken, UpdatePatientProfileRequest body, RepoCallback cb) {
        RetrofitClient.patientProfileApi().updateMe(bearerToken, body).enqueue(new Callback<ApiEnvelope>() {
            @Override
            public void onResponse(Call<ApiEnvelope> call, Response<ApiEnvelope> response) {
                ApiEnvelope env = response.body();
                if (response.isSuccessful() && env != null && Boolean.TRUE.equals(env.success)) {
                    cb.onSuccess(env);
                } else {
                    cb.onError(extractMsg(response, env));
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope> call, Throwable t) {
                cb.onError(t != null && t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /**
     * Lấy hồ sơ bệnh nhân hiện tại từ DB (GET /api/v1/patient/profile).
     * Dùng cho trường hợp logout/login để tự nạp lại ProfileExtras.
     */
    @androidx.annotation.Nullable
    public ProfileExtras meSync(String bearerToken) {
        try {
            Response<ApiEnvelope> response = RetrofitClient.patientProfileApi().me(bearerToken).execute();
            ApiEnvelope env = response.body();
            if (response.isSuccessful() && env != null && Boolean.TRUE.equals(env.success)
                    && env.data != null && !env.data.isJsonNull()) {
                PatientProfileResponseDto dto = RetrofitClient.gson().fromJson(env.data, PatientProfileResponseDto.class);
                if (dto == null) return null;
                return new ProfileExtras(
                        dto.phone != null ? dto.phone : "",
                        dto.dob != null ? dto.dob : "",
                        dto.gender != null ? dto.gender : "",
                        dto.address != null ? dto.address : ""
                );
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Async variant of {@link #meSync(String)} for UI screens. */
    public void fetchMe(String bearerToken, ProfileExtrasCallback cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = RetrofitClient.patientProfileApi().me(bearerToken).execute();
                ApiEnvelope env = response.body();
                if (response.isSuccessful() && env != null && Boolean.TRUE.equals(env.success)
                        && env.data != null && !env.data.isJsonNull()) {
                    PatientProfileResponseDto dto = RetrofitClient.gson().fromJson(env.data, PatientProfileResponseDto.class);
                    if (dto == null) {
                        cb.onDone(null, "Không parse được hồ sơ");
                        return;
                    }
                    ProfileExtras ex = new ProfileExtras(
                            dto.phone != null ? dto.phone : "",
                            dto.dob != null ? dto.dob : "",
                            dto.gender != null ? dto.gender : "",
                            dto.address != null ? dto.address : ""
                    );
                    cb.onDone(ex, null);
                    return;
                }
                cb.onDone(null, extractMsg(response, env));
            } catch (Exception e) {
                cb.onDone(null, e != null && e.getMessage() != null ? e.getMessage() : "Network error");
            }
        });
    }

    /**
     * Lấy đúng `patientId` để hiển thị mã bệnh nhân trên ticket.
     * (Không dùng accountId vì `patients.id` khác với `users.userId`)
     */
    public void fetchPatientId(String bearerToken, PatientIdCallback cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = RetrofitClient.patientProfileApi().me(bearerToken).execute();
                ApiEnvelope env = response.body();
                if (response.isSuccessful() && env != null && Boolean.TRUE.equals(env.success)
                        && env.data != null && !env.data.isJsonNull()) {
                    PatientProfileResponseDto dto = RetrofitClient.gson().fromJson(env.data, PatientProfileResponseDto.class);
                    Integer id = dto != null ? dto.patientId : null;
                    cb.onDone(id, null);
                    return;
                }
                cb.onDone(null, extractMsg(response, env));
            } catch (Exception e) {
                cb.onDone(null, e != null && e.getMessage() != null ? e.getMessage() : "Network error");
            }
        });
    }

    private static String extractMsg(Response<ApiEnvelope> response, @Nullable ApiEnvelope env) {
        if (env != null && env.message != null) return env.message;
        return "HTTP " + response.code();
    }
}

