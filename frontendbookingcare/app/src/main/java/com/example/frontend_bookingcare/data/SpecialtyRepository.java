package com.example.frontend_bookingcare.data;

import android.os.Handler;
import android.os.Looper;

import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.RetrofitClient;
import com.example.frontend_bookingcare.util.RepoMessages;
import com.example.frontend_bookingcare.api.SpecialtyApiService;
import com.example.frontend_bookingcare.api.SpecialtyDto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class SpecialtyRepository {

    public interface ResultCallback<T> {
        void onDone(T data, String errorMessage);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final SpecialtyApiService api = RetrofitClient.specialtyApi();
    private final Gson gson = RetrofitClient.gson();

    public void fetchAllSpecialties(ResultCallback<List<SpecialtyDto>> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.getAllSpecialties().execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    List<SpecialtyDto> list = Collections.emptyList();
                    if (body.data != null && !body.data.isJsonNull()) {
                        Type t = new TypeToken<List<SpecialtyDto>>() {
                        }.getType();
                        list = gson.fromJson(body.data, t);
                        if (list == null) list = Collections.emptyList();
                    }
                    final List<SpecialtyDto> result = list;
                    MAIN.post(() -> cb.onDone(result, null));
                } else {
                    String msg = body != null && body.message != null && !body.message.isEmpty()
                            ? body.message : RepoMessages.httpError(response.code());
                    MAIN.post(() -> cb.onDone(null, msg));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, RepoMessages.networkError()));
            }
        });
    }
}
