package com.example.frontend_bookingcare.data;

import android.os.Handler;
import android.os.Looper;

import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.ClinicalNoteRequest;
import com.example.frontend_bookingcare.api.DoctorAppointmentDetailDto;
import com.example.frontend_bookingcare.api.DoctorAppointmentDto;
import com.example.frontend_bookingcare.api.DoctorProfileDto;
import com.example.frontend_bookingcare.api.DoctorPanelApiService;
import com.example.frontend_bookingcare.api.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class DoctorPanelRepository {

    public interface ResultCallback<T> {
        void onDone(T data, String errorMessage);
    }

    public static final class TabLists {
        public final List<DoctorAppointmentDto> pending;
        public final List<DoctorAppointmentDto> confirmed;
        public final List<DoctorAppointmentDto> cancelled;

        public TabLists(List<DoctorAppointmentDto> pending,
                        List<DoctorAppointmentDto> confirmed,
                        List<DoctorAppointmentDto> cancelled) {
            this.pending = pending;
            this.confirmed = confirmed;
            this.cancelled = cancelled;
        }
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final DoctorPanelApiService api = RetrofitClient.doctorPanelApi();
    private final Gson gson = RetrofitClient.gson();

    public void fetchAllTabs(String authorizationBearer, ResultCallback<TabLists> cb) {
        EXECUTOR.execute(() -> {
            try {
                List<DoctorAppointmentDto> pending = fetchStatusBlocking(authorizationBearer, "PENDING");
                List<DoctorAppointmentDto> confirmed = fetchStatusBlocking(authorizationBearer, "CONFIRMED");
                List<DoctorAppointmentDto> cancelled = fetchStatusBlocking(authorizationBearer, "CANCELLED");
                TabLists out = new TabLists(pending, confirmed, cancelled);
                MAIN.post(() -> cb.onDone(out, null));
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    public void fetchToday(String authorizationBearer, ResultCallback<List<DoctorAppointmentDto>> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.getTodayAppointments(authorizationBearer).execute();
                List<DoctorAppointmentDto> list = unwrapList(response);
                MAIN.post(() -> cb.onDone(list, null));
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    public void confirmAppointment(String authorizationBearer, int appointmentId, ResultCallback<Boolean> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.confirmAppointment(authorizationBearer, appointmentId).execute();
                boolean ok = response.isSuccessful() && response.body() != null && response.body().success;
                MAIN.post(() -> cb.onDone(ok, ok ? null : errorMessage(response.body(), response.code())));
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(false, safeMessage(e)));
            }
        });
    }

    public void cancelAppointment(String authorizationBearer, int appointmentId, ResultCallback<Boolean> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.cancelAppointment(authorizationBearer, appointmentId).execute();
                boolean ok = response.isSuccessful() && response.body() != null && response.body().success;
                MAIN.post(() -> cb.onDone(ok, ok ? null : errorMessage(response.body(), response.code())));
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(false, safeMessage(e)));
            }
        });
    }

    public void markNoShow(String authorizationBearer, int appointmentId, ResultCallback<DoctorAppointmentDetailDto> cb) {
        patchDetail(authorizationBearer, appointmentId, auth -> api.markNoShow(auth, appointmentId), cb);
    }

    public void startExam(String authorizationBearer, int appointmentId, ResultCallback<DoctorAppointmentDetailDto> cb) {
        patchDetail(authorizationBearer, appointmentId, auth -> api.startExam(auth, appointmentId), cb);
    }

    public void saveClinicalNote(String authorizationBearer, int appointmentId, String note,
                                 ResultCallback<DoctorAppointmentDetailDto> cb) {
        patchDetail(authorizationBearer, appointmentId,
                auth -> api.saveClinicalNote(auth, appointmentId, new ClinicalNoteRequest(note)), cb);
    }

    public void completeExam(String authorizationBearer, int appointmentId, String note,
                             ResultCallback<DoctorAppointmentDetailDto> cb) {
        patchDetail(authorizationBearer, appointmentId,
                auth -> api.completeExam(auth, appointmentId, new ClinicalNoteRequest(note)), cb);
    }

    private void patchDetail(String authorizationBearer, int appointmentId,
                             PatchCall call, ResultCallback<DoctorAppointmentDetailDto> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = call.execute(authorizationBearer).execute();
                DoctorAppointmentDetailDto dto = unwrapDetail(response);
                if (dto != null) {
                    MAIN.post(() -> cb.onDone(dto, null));
                } else {
                    MAIN.post(() -> cb.onDone(null, errorMessage(response.body(), response.code())));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    private interface PatchCall {
        retrofit2.Call<ApiEnvelope> execute(String authorizationBearer);
    }

    /**
     * Chi tiết cuộc hẹn (ngày sinh / giới tính bệnh nhân). Trả null nếu HTTP lỗi hoặc không có dữ liệu — không ném exception.
     */
    public void fetchAppointmentDetail(String authorizationBearer, int appointmentId,
                                       ResultCallback<DoctorAppointmentDetailDto> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.getAppointmentDetail(authorizationBearer, appointmentId).execute();
                DoctorAppointmentDetailDto dto = unwrapDetail(response);
                MAIN.post(() -> cb.onDone(dto, null));
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    private DoctorAppointmentDetailDto unwrapDetail(Response<ApiEnvelope> response) {
        ApiEnvelope body = response.body();
        if (!response.isSuccessful() || body == null || !body.success) {
            return null;
        }
        if (body.data == null || body.data.isJsonNull()) {
            return null;
        }
        return gson.fromJson(body.data, DoctorAppointmentDetailDto.class);
    }

    private List<DoctorAppointmentDto> fetchStatusBlocking(String auth, String status) throws IOException {
        Response<ApiEnvelope> response = api.getAppointmentsByStatus(auth, status).execute();
        return unwrapList(response);
    }

    public void fetchProfile(String authorizationBearer, ResultCallback<DoctorProfileDto> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.getProfile(authorizationBearer).execute();
                ApiEnvelope body = response.body();
                if (!response.isSuccessful() || body == null || !body.success) {
                    MAIN.post(() -> cb.onDone(null, errorMessage(body, response.code())));
                    return;
                }
                if (body.data == null || body.data.isJsonNull()) {
                    MAIN.post(() -> cb.onDone(null, "Không có dữ liệu hồ sơ"));
                    return;
                }
                DoctorProfileDto dto = gson.fromJson(body.data, DoctorProfileDto.class);
                MAIN.post(() -> cb.onDone(dto, null));
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    private List<DoctorAppointmentDto> unwrapList(Response<ApiEnvelope> response) {
        ApiEnvelope body = response.body();
        if (!response.isSuccessful() || body == null || !body.success) {
            throw new IllegalStateException(errorMessage(body, response.code()));
        }
        if (body.data == null || body.data.isJsonNull()) {
            return Collections.emptyList();
        }
        Type t = new TypeToken<List<DoctorAppointmentDto>>() {
        }.getType();
        List<DoctorAppointmentDto> list = gson.fromJson(body.data, t);
        return list != null ? list : Collections.emptyList();
    }

    private static String errorMessage(ApiEnvelope body, int httpCode) {
        if (body != null && body.message != null && !body.message.isEmpty()) return body.message;
        return "HTTP " + httpCode;
    }

    private static String safeMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Network error";
    }
}
