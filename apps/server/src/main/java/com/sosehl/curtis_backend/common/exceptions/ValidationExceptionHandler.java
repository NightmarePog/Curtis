package com.sosehl.curtis_backend.common.exceptions;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(
        MethodArgumentNotValidException ex
    ) {
        Map<String, Object> body = new HashMap<>();
        var errors = ex
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(
                f ->
                    f.getObjectName() +
                    "_" +
                    f.getField() +
                    ": " +
                    f.getDefaultMessage()
            )
            .collect(Collectors.toList());

        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
