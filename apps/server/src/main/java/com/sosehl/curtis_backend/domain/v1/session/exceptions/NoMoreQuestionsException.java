package com.sosehl.curtis_backend.domain.v1.session.exceptions;

public class NoMoreQuestionsException extends RuntimeException {

    public NoMoreQuestionsException(String message) {
        super(message);
    }
}
