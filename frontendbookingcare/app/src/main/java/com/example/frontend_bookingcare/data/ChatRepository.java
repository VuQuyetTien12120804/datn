package com.example.frontend_bookingcare.data;

import android.os.Handler;
import android.os.Looper;

import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.ChatMessageDto;
import com.example.frontend_bookingcare.api.ChatThreadDto;
import com.example.frontend_bookingcare.api.DoctorChatApiService;
import com.example.frontend_bookingcare.api.PatientChatApiService;
import com.example.frontend_bookingcare.api.RetrofitClient;
import com.example.frontend_bookingcare.util.RepoMessages;
import com.example.frontend_bookingcare.api.SendChatMessageRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class ChatRepository {

    public enum Audience {
        PATIENT,
        DOCTOR
    }

    public interface ResultCallback<T> {
        void onDone(T data, String errorMessage);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final PatientChatApiService patientApi = RetrofitClient.patientChatApi();
    private final DoctorChatApiService doctorApi = RetrofitClient.doctorChatApi();
    private final Gson gson = RetrofitClient.gson();

    public void fetchThreads(Audience audience, String authorizationBearer, ResultCallback<List<ChatThreadDto>> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = audience == Audience.DOCTOR
                        ? doctorApi.listThreads(authorizationBearer).execute()
                        : patientApi.listThreads(authorizationBearer).execute();
                handleList(response, cb, ChatThreadDto.class);
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    public void fetchMessages(Audience audience, String authorizationBearer, String threadKey,
                              ResultCallback<List<ChatMessageDto>> cb) {
        EXECUTOR.execute(() -> {
            try {
                String encoded = encodeThreadKey(threadKey);
                Response<ApiEnvelope> response = audience == Audience.DOCTOR
                        ? doctorApi.listMessages(authorizationBearer, encoded).execute()
                        : patientApi.listMessages(authorizationBearer, encoded).execute();
                handleList(response, cb, ChatMessageDto.class);
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    public void sendMessage(Audience audience, String authorizationBearer, String threadKey, String content,
                            ResultCallback<ChatMessageDto> cb) {
        EXECUTOR.execute(() -> {
            try {
                String encoded = encodeThreadKey(threadKey);
                SendChatMessageRequest body = new SendChatMessageRequest(content);
                Response<ApiEnvelope> response = audience == Audience.DOCTOR
                        ? doctorApi.sendMessage(authorizationBearer, encoded, body).execute()
                        : patientApi.sendMessage(authorizationBearer, encoded, body).execute();
                handleSingle(response, cb, ChatMessageDto.class);
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    public void markRead(Audience audience, String authorizationBearer, String threadKey, ResultCallback<Void> cb) {
        EXECUTOR.execute(() -> {
            try {
                String encoded = encodeThreadKey(threadKey);
                Response<ApiEnvelope> response = audience == Audience.DOCTOR
                        ? doctorApi.markRead(authorizationBearer, encoded).execute()
                        : patientApi.markRead(authorizationBearer, encoded).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    MAIN.post(() -> cb.onDone(null, null));
                } else {
                    MAIN.post(() -> cb.onDone(null, errorMessage(response)));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    private <T> void handleList(Response<ApiEnvelope> response, ResultCallback<List<T>> cb, Class<T> elementClass) {
        ApiEnvelope body = response.body();
        if (response.isSuccessful() && body != null && body.success) {
            List<T> list = Collections.emptyList();
            if (body.data != null && !body.data.isJsonNull()) {
                Type t = TypeToken.getParameterized(List.class, elementClass).getType();
                list = gson.fromJson(body.data, t);
                if (list == null) list = Collections.emptyList();
            }
            List<T> out = list;
            MAIN.post(() -> cb.onDone(out, null));
        } else {
            MAIN.post(() -> cb.onDone(null, errorMessage(response)));
        }
    }

    private <T> void handleSingle(Response<ApiEnvelope> response, ResultCallback<T> cb, Class<T> clazz) {
        ApiEnvelope body = response.body();
        if (response.isSuccessful() && body != null && body.success) {
            T item = null;
            if (body.data != null && !body.data.isJsonNull()) {
                item = gson.fromJson(body.data, clazz);
            }
            T out = item;
            MAIN.post(() -> cb.onDone(out, null));
        } else {
            MAIN.post(() -> cb.onDone(null, errorMessage(response)));
        }
    }

    private static String encodeThreadKey(String threadKey) {
        if (threadKey == null) return "";
        return URLEncoder.encode(threadKey, StandardCharsets.UTF_8);
    }

    private static String errorMessage(Response<ApiEnvelope> response) {
        ApiEnvelope body = response.body();
        if (body != null && body.message != null && !body.message.isEmpty()) {
            return body.message;
        }
        if (response.errorBody() != null) {
            try {
                ApiEnvelope err = RetrofitClient.gson().fromJson(response.errorBody().charStream(), ApiEnvelope.class);
                if (err != null && err.message != null && !err.message.isEmpty()) {
                    return err.message;
                }
            } catch (Exception ignored) {
            }
        }
        return RepoMessages.httpError(response.code());
    }

    private static String safeMessage(Exception e) {
        return RepoMessages.networkError();
    }
}
