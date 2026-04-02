package com.sosehl.curtis_backend.domain.v1.session.exceptions;

public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException(String message) {
        super(message);
    }
}
