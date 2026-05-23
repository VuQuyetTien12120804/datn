package com.bookingcare.backend_bookingcare.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiEnvelope<T>(
        boolean success,
        int code,
        String message,
        T data
) {
    public static <T> ApiEnvelope<T> ok(T data) {
        return new ApiEnvelope<>(true, 200, "OK", data);
    }

    public static <T> ApiEnvelope<T> ok(String message, T data) {
        return new ApiEnvelope<>(true, 200, message, data);
    }

    public static <T> ApiEnvelope<T> fail(int code, String message) {
        return new ApiEnvelope<>(false, code, message, null);
    }
}
