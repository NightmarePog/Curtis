package com.sosehl.curtis_backend.common.exceptions;

import com.sosehl.curtis_backend.domain.v1.session.exceptions.NoMoreQuestionsException;
import com.sosehl.curtis_backend.domain.v1.session.exceptions.SessionExpiredException;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(
        GlobalExceptionHandler.class
    );

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
        ResponseStatusException ex
    ) {
        log.error("ResponseStatusException: {}", ex.getReason(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("statusCode", ex.getStatusCode().value());
        body.put("error", "Response status exception");
        body.put("message", ex.getReason());
        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleSessionExpired(
        SessionExpiredException ex
    ) {
        log.error("SessionExpiredException: {}", ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("statusCode", HttpStatus.GONE.value());
        body.put("error", "Session expired");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.GONE);
    }

    @ExceptionHandler(NoMoreQuestionsException.class)
    public ResponseEntity<Map<String, Object>> handleNoMoreQuestions(
        NoMoreQuestionsException ex
    ) {
        log.error("NoMoreQuestionsException: {}", ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("statusCode", HttpStatus.BAD_REQUEST.value());
        body.put("error", "No more questions");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
        MethodArgumentNotValidException ex
    ) {
        log.error("Validation error: {}", ex.getMessage(), ex);

        Map<String, String> fieldErrors = new HashMap<>();
        ex
            .getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
            );

        Map<String, Object> body = new HashMap<>();
        body.put("statusCode", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("message", fieldErrors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
        ConstraintViolationException ex
    ) {
        log.error("ConstraintViolationException: {}", ex.getMessage(), ex);

        Map<String, String> violations = new HashMap<>();
        ex
            .getConstraintViolations()
            .forEach(v ->
                violations.put(v.getPropertyPath().toString(), v.getMessage())
            );

        Map<String, Object> body = new HashMap<>();
        body.put("statusCode", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Constraint violation");
        body.put("message", violations);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
        Exception ex
    ) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal server error");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
