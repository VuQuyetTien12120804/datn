package com.example.frontend_bookingcare.data;

import android.os.Handler;
import android.os.Looper;

import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.LegalApiService;
import com.example.frontend_bookingcare.api.LegalDocumentDto;
import com.example.frontend_bookingcare.api.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class LegalRepository {

    public interface ResultCallback {
        void onDone(LegalDocumentDto data, String errorMessage);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final LegalApiService api = RetrofitClient.legalApi();
    private final Gson gson = RetrofitClient.gson();

    public void fetchByCode(String code, ResultCallback cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.getByCode(code).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success && body.data != null && !body.data.isJsonNull()) {
                    Type t = new TypeToken<LegalDocumentDto>() {
                    }.getType();
                    LegalDocumentDto dto = gson.fromJson(body.data, t);
                    MAIN.post(() -> cb.onDone(dto, null));
                } else {
                    String msg = (body != null && body.message != null && !body.message.isEmpty())
                            ? body.message
                            : ("HTTP " + response.code());
                    MAIN.post(() -> cb.onDone(null, msg));
                }
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Network error";
                MAIN.post(() -> cb.onDone(null, msg));
            }
        });
    }

    /**
     * 4xx/5xx: Retrofit thường trả body=null; message nằm trong {@link Response#errorBody()}.
     */
    private String httpErrorMessage(Response<ApiEnvelope> response, ApiEnvelope successBody) {
        if (successBody != null && successBody.message != null && !successBody.message.isEmpty()) {
            return successBody.message;
        }
        try {
            ResponseBody err = response.errorBody();
            if (err != null) {
                String s = err.string();
                if (s != null && !s.isEmpty()) {
                    ApiEnvelope parsed = gson.fromJson(s, ApiEnvelope.class);
                    if (parsed != null && parsed.message != null && !parsed.message.isEmpty()) {
                        return parsed.message;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "HTTP " + response.code();
    }
}
