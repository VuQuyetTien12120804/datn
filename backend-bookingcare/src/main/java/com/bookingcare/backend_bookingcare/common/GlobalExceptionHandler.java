package com.bookingcare.backend_bookingcare.common;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getCode()).body(ApiEnvelope.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError first = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String msg = first != null ? first.getField() + ": " + first.getDefaultMessage() : "Validation failed";
        return ResponseEntity.badRequest().body(ApiEnvelope.fail(400, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiEnvelope<Void>> handleOther(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(ApiEnvelope.fail(500, ex.getMessage() != null ? ex.getMessage() : "Internal server error"));
    }
}
