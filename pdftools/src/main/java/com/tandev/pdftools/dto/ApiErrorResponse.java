package com.tandev.pdftools.dto;

import java.time.Instant;

/**
 * Standard error payload returned by {@link com.tandev.pdftools.exception.GlobalExceptionHandler}.
 */
public record ApiErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp) {

    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(status, error, message, Instant.now());
    }
}
