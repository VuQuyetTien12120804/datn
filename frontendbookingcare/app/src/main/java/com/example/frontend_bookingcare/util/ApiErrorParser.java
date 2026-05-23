package com.example.frontend_bookingcare.util;

import androidx.annotation.Nullable;

import com.example.frontend_bookingcare.api.ApiEnvelope;
import com.example.frontend_bookingcare.api.RetrofitClient;

import retrofit2.Response;

/** Parse backend error messages from envelope body or HTTP error body. */
public final class ApiErrorParser {

    private ApiErrorParser() {
    }

    public static String message(Response<ApiEnvelope> response) {
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

    /** When caller already extracted {@code body} from a successful parse attempt. */
    public static String message(@Nullable ApiEnvelope body, Response<ApiEnvelope> response) {
        if (body != null && body.message != null && !body.message.isEmpty()) {
            return body.message;
        }
        return message(response);
    }
}
