package com.example.frontend_bookingcare.data;

import android.os.Handler;
import android.os.Looper;

import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.DoctorApiService;
import com.example.frontend_bookingcare.api.DoctorDto;
import com.example.frontend_bookingcare.api.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class DoctorRepository {

    public interface ResultCallback<T> {
        void onDone(T data, String errorMessage);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final DoctorApiService api = RetrofitClient.doctorApi();
    private final Gson gson = RetrofitClient.gson();

    public void fetchAllDoctors(ResultCallback<List<DoctorDto>> cb) {
        executeDoctorCall(api.getAllDoctors(), cb);
    }

    /**
     * Gọi đúng endpoint GET /api/v1/doctors/specialty/{id} thay vì lấy tất cả bác sĩ
     * rồi lọc client-side. Trả về danh sách bác sĩ thuộc chuyên khoa đó.
     */
    public void fetchDoctorsBySpecialty(int specialtyId, ResultCallback<List<DoctorDto>> cb) {
        executeDoctorCall(api.getDoctorsBySpecialty(specialtyId), cb);
    }

    private void executeDoctorCall(retrofit2.Call<ApiEnvelope> call, ResultCallback<List<DoctorDto>> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = call.execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    List<DoctorDto> list = Collections.emptyList();
                    if (body.data != null && !body.data.isJsonNull()) {
                        Type t = new TypeToken<List<DoctorDto>>() {
                        }.getType();
                        list = gson.fromJson(body.data, t);
                        if (list == null) list = Collections.emptyList();
                    }
                    final List<DoctorDto> result = list;
                    MAIN.post(() -> cb.onDone(result, null));
                } else {
                    String msg = body != null ? body.message : ("HTTP " + response.code());
                    MAIN.post(() -> cb.onDone(null, msg));
                }
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Network error";
                MAIN.post(() -> cb.onDone(null, msg));
            }
        });
    }
}
