package com.sauda.exception;

import com.sauda.dto.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SaudaNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSaudaNotFoundException(
            SaudaNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(HttpStatus.NOT_FOUND, exception.getMessage(), request));
    }

    @ExceptionHandler(SaudaException.class)
    public ResponseEntity<ApiErrorResponse> handleSaudaException(
            SaudaException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(errorBody(HttpStatus.BAD_REQUEST, exception.getMessage(), request));
    }

    @ExceptionHandler(SaudaUnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleSaudaUnauthorizedException(
            SaudaUnauthorizedException exception, HttpServletRequest request) {
        log.warn(
                "Unauthorized request: path={}, message={}",
                request.getRequestURI(),
                exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody(HttpStatus.UNAUTHORIZED, exception.getMessage(), request));
    }

    @ExceptionHandler({AuthenticationException.class})
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException exception, HttpServletRequest request) {
        log.warn(
                "Authentication failed: path={}, message={}",
                request.getRequestURI(),
                exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody(HttpStatus.UNAUTHORIZED, "Authentication required", request));
    }

    @ExceptionHandler({SaudaForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleForbiddenException(
            RuntimeException exception, HttpServletRequest request) {
        log.warn(
                "Access denied: path={}, message={}",
                request.getRequestURI(),
                exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(HttpStatus.FORBIDDEN, "Access denied", request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .findFirst()
                        .orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(errorBody(HttpStatus.BAD_REQUEST, message, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception exception, HttpServletRequest request) {
        log.error("Unexpected error: path={}", request.getRequestURI(), exception);
        return ResponseEntity.internalServerError()
                .body(
                        errorBody(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "An unexpected error occurred",
                                request));
    }

    private static ApiErrorResponse errorBody(
            HttpStatus status, String message, HttpServletRequest request) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
    }
}
