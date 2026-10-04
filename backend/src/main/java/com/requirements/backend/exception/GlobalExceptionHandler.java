package com.requirements.backend.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // =====================================================
    // Handle Illegal State Errors
    // =====================================================

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException exception) {

        Map<String, Object> response =
                createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }


    // =====================================================
    // Handle Runtime Errors
    // =====================================================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException exception) {

        Map<String, Object> response =
                createErrorResponse(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }


    // =====================================================
    // Handle Unexpected Errors
    // =====================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception exception) {

        Map<String, Object> response =
                createErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected server error occurred."
                );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }


    // =====================================================
    // Build Standard Error Response
    // =====================================================

    private Map<String, Object> createErrorResponse(
            HttpStatus status,
            String message) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        response.put(
                "status",
                status.value()
        );

        response.put(
                "error",
                status.getReasonPhrase()
        );

        response.put(
                "message",
                message
        );

        return response;
    }
}