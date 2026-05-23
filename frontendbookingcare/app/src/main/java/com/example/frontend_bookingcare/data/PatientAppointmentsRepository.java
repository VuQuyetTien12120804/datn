package com.example.frontend_bookingcare.data;

import android.os.Handler;
import android.os.Looper;

import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.CancelAppointmentRequest;
import com.example.frontend_bookingcare.api.PatientAppointmentDto;
import com.example.frontend_bookingcare.api.PatientAppointmentsApiService;
import com.example.frontend_bookingcare.api.RetrofitClient;
import com.example.frontend_bookingcare.util.ApiErrorParser;
import com.example.frontend_bookingcare.util.RepoMessages;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class PatientAppointmentsRepository {
    public interface ResultCallback<T> {
        void onDone(T data, String errorMessage);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final PatientAppointmentsApiService api = RetrofitClient.patientAppointmentsApi();
    private final Gson gson = RetrofitClient.gson();

    public void fetch(String authorizationBearer, String group, ResultCallback<List<PatientAppointmentDto>> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.myAppointments(authorizationBearer, group).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    List<PatientAppointmentDto> list = Collections.emptyList();
                    if (body.data != null && !body.data.isJsonNull()) {
                        Type t = new TypeToken<List<PatientAppointmentDto>>() {
                        }.getType();
                        list = gson.fromJson(body.data, t);
                        if (list == null) list = Collections.emptyList();
                    }
                    List<PatientAppointmentDto> out = list;
                    MAIN.post(() -> cb.onDone(out, null));
                } else {
                    MAIN.post(() -> cb.onDone(null, ApiErrorParser.message(response)));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    public void fetchDetail(String authorizationBearer, int appointmentId, ResultCallback<PatientAppointmentDto> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.appointmentDetail(authorizationBearer, appointmentId).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success && body.data != null && !body.data.isJsonNull()) {
                    PatientAppointmentDto dto = gson.fromJson(body.data, PatientAppointmentDto.class);
                    MAIN.post(() -> cb.onDone(dto, null));
                } else {
                    MAIN.post(() -> cb.onDone(null, ApiErrorParser.message(response)));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    public void cancel(String authorizationBearer, int appointmentId, String cancelReason, ResultCallback<ApiEnvelope> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.cancelAppointment(
                        authorizationBearer,
                        appointmentId,
                        new CancelAppointmentRequest(cancelReason)
                ).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    MAIN.post(() -> cb.onDone(body, null));
                } else {
                    MAIN.post(() -> cb.onDone(null, ApiErrorParser.message(response)));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    public void checkIn(String authorizationBearer, int appointmentId, ResultCallback<Boolean> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.checkInAppointment(authorizationBearer, appointmentId).execute();
                boolean ok = response.isSuccessful() && response.body() != null && response.body().success;
                MAIN.post(() -> cb.onDone(ok, ok ? null : ApiErrorParser.message(response)));
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(false, safeMessage(e)));
            }
        });
    }
    private static String safeMessage(Exception e) {
        return RepoMessages.networkError();
    }
}

