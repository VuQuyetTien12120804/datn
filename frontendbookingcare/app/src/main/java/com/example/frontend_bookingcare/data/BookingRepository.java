package com.example.frontend_bookingcare.data;

import android.os.Handler;
import android.os.Looper;

import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.BookingApiService;
import com.example.frontend_bookingcare.api.BookingRequest;
import com.example.frontend_bookingcare.api.BookingResponseDto;
import com.example.frontend_bookingcare.api.RetrofitClient;
import com.example.frontend_bookingcare.api.SlotDto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

/**
 * Repository cho 3 endpoint đặt lịch (working-dates / slots / book).
 * Tất cả I/O chạy trên {@link #EXECUTOR} và callback được post về main thread
 * để Activity/Fragment update UI an toàn.
 */
public class BookingRepository {

    public interface ResultCallback<T> {
        void onDone(T data, String errorMessage);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final BookingApiService api = RetrofitClient.bookingApi();
    private final Gson gson = RetrofitClient.gson();

    /** GET /api/v1/booking/working-dates — trả về danh sách ngày (ISO "yyyy-MM-dd"). */
    public void fetchWorkingDates(int doctorId, ResultCallback<List<String>> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.getWorkingDates(doctorId).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    List<String> list = Collections.emptyList();
                    if (body.data != null && !body.data.isJsonNull()) {
                        Type t = new TypeToken<List<String>>() {
                        }.getType();
                        list = gson.fromJson(body.data, t);
                        if (list == null) list = Collections.emptyList();
                    }
                    final List<String> result = list;
                    MAIN.post(() -> cb.onDone(result, null));
                } else {
                    MAIN.post(() -> cb.onDone(null, errorMessage(body, response.code())));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    /** GET /api/v1/booking/slots?doctorId=...&slotDate=yyyy-MM-dd. */
    public void fetchSlots(int doctorId, String slotDate, ResultCallback<List<SlotDto>> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.getSlots(doctorId, slotDate).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    List<SlotDto> list = Collections.emptyList();
                    if (body.data != null && !body.data.isJsonNull()) {
                        Type t = new TypeToken<List<SlotDto>>() {
                        }.getType();
                        list = gson.fromJson(body.data, t);
                        if (list == null) list = Collections.emptyList();
                    }
                    final List<SlotDto> result = list;
                    MAIN.post(() -> cb.onDone(result, null));
                } else {
                    MAIN.post(() -> cb.onDone(null, errorMessage(body, response.code())));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    /** POST /api/v1/booking/book — tạo appointment. */
    public void book(String bearerToken, BookingRequest request, ResultCallback<BookingResponseDto> cb) {
        EXECUTOR.execute(() -> {
            try {
                Response<ApiEnvelope> response = api.book(bearerToken, request).execute();
                ApiEnvelope body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    BookingResponseDto result = null;
                    if (body.data != null && !body.data.isJsonNull()) {
                        result = gson.fromJson(body.data, BookingResponseDto.class);
                    }
                    final BookingResponseDto out = result;
                    MAIN.post(() -> cb.onDone(out, null));
                } else {
                    MAIN.post(() -> cb.onDone(null, errorMessage(body, response.code())));
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onDone(null, safeMessage(e)));
            }
        });
    }

    private static String errorMessage(ApiEnvelope body, int httpCode) {
        if (body != null && body.message != null && !body.message.isEmpty()) return body.message;
        return "HTTP " + httpCode;
    }

    private static String safeMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Network error";
    }
}
