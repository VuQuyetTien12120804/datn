package com.example.frontend_bookingcare.data;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.ChatbotApiService;
import com.example.frontend_bookingcare.api.ChatbotMessageDto;
import com.example.frontend_bookingcare.api.ChatbotSendBody;
import com.example.frontend_bookingcare.api.RetrofitClient;
import com.example.frontend_bookingcare.util.ApiErrorParser;
import com.example.frontend_bookingcare.util.RepoMessages;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

/**
 * Repository gọi 3 endpoint chatbot. Tất cả I/O chạy trên executor riêng;
 * callback được post về main thread để Activity update UI an toàn.
 */
public class ChatbotRepository {

    public interface ResultCallback<T> {
        void onDone(@Nullable T data, @Nullable String error);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final ChatbotApiService api = RetrofitClient.chatbotApi();
    private final Gson gson = RetrofitClient.gson();

    /** Mở session — trả về sessionId hoặc lỗi. */
    public void openSession(String bearer, ResultCallback<Long> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> r = api.openSession(bearer).execute();
                ApiEnvelope body = r.body();
                if (r.isSuccessful() && body != null && body.success
                        && body.data != null && body.data.isJsonObject()) {
                    JsonObject obj = body.data.getAsJsonObject();
                    long sessionId = obj.has("sessionId") ? obj.get("sessionId").getAsLong() : -1L;
                    if (sessionId <= 0) {
                        MAIN.post(() -> cb.onDone(null, RepoMessages.networkError()));
                        return;
                    }
                    MAIN.post(() -> cb.onDone(sessionId, null));
                } else {
                    MAIN.post(() -> cb.onDone(null, ApiErrorParser.message(r)));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    /** Lấy lịch sử tin nhắn trong session. */
    public void listMessages(String bearer, long sessionId, ResultCallback<List<ChatbotMessageDto>> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> r = api.listMessages(bearer, sessionId).execute();
                ApiEnvelope body = r.body();
                if (r.isSuccessful() && body != null && body.success) {
                    List<ChatbotMessageDto> list = parseMessageList(body);
                    MAIN.post(() -> cb.onDone(list, null));
                } else {
                    MAIN.post(() -> cb.onDone(null, ApiErrorParser.message(r)));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    /** Gửi 1 câu hỏi — backend trả 2 message: user echo + assistant reply. */
    public void sendMessage(String bearer, long sessionId, String content,
                            ResultCallback<List<ChatbotMessageDto>> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> r = api.sendMessage(bearer, sessionId,
                        new ChatbotSendBody(content)).execute();
                ApiEnvelope body = r.body();
                if (r.isSuccessful() && body != null && body.success) {
                    List<ChatbotMessageDto> list = parseMessageList(body);
                    MAIN.post(() -> cb.onDone(list, null));
                } else {
                    MAIN.post(() -> cb.onDone(null, ApiErrorParser.message(r)));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    private List<ChatbotMessageDto> parseMessageList(ApiEnvelope body) {
        if (body.data == null || body.data.isJsonNull()) return Collections.emptyList();
        Type t = new TypeToken<List<ChatbotMessageDto>>() {}.getType();
        List<ChatbotMessageDto> out = gson.fromJson(body.data, t);
        return out != null ? out : Collections.emptyList();
    }

    private static String safeMessage(Exception e) {
        android.util.Log.e("ChatbotRepository", "Chatbot call failed", e);
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            return RepoMessages.networkError() + " — " + e.getMessage();
        }
        return RepoMessages.networkError();
    }
}
