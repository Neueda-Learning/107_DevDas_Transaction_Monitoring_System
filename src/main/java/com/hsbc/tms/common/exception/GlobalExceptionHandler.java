package com.hsbc.tms.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());

        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", List.of(ex.getMessage()));
    }

    private ResponseEntity<ApiErrorResponse> buildError(HttpStatus status, String message, List<String> details) {
        ApiErrorResponse response = new ApiErrorResponse();
        response.setTimestamp(Instant.now());
        response.setStatus(status.value());
        response.setError(status.getReasonPhrase());
        response.setMessage(message);
        response.setDetails(details);
        return ResponseEntity.status(status).body(response);
    }
}
