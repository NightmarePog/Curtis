package com.sosehl.curtis_backend.common.exceptions;

import com.sosehl.curtis_backend.domain.v1.session.exceptions.NoMoreQuestionsException;
import com.sosehl.curtis_backend.domain.v1.session.exceptions.SessionExpiredException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
        ResponseStatusException ex
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("statusCode", ex.getStatusCode().value());
        body.put("reason", ex.getReason());
        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<String> handleSessionExpired(
        SessionExpiredException ex
    ) {
        return ResponseEntity.status(HttpStatus.GONE).body(ex.getMessage());
    }

    @ExceptionHandler(NoMoreQuestionsException.class)
    public ResponseEntity<String> handleNoMoreQuestions(
        NoMoreQuestionsException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ex.getMessage()
        );
    }
}
