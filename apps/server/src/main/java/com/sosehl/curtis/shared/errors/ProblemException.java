package com.sosehl.curtis.shared.errors;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

@SuppressWarnings("serial")
public final class ProblemException extends ErrorResponseException {

    private final HttpStatus status;
    private final String code;
    private final String detail;

    public ProblemException(HttpStatus status, String code, String detail) {
        super(status, problem(status, code, detail), null);
        this.status = status;
        this.code = code;
        this.detail = detail;
    }

    private static ProblemDetail problem(
        HttpStatus status,
        String code,
        String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            status,
            detail
        );
        problem.setType(URI.create("urn:curtis:error:" + code));
        problem.setProperty("code", code);
        return problem;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    @Override
    public String getMessage() {
        return detail;
    }

    public static ProblemException badRequest(String code, String detail) {
        return new ProblemException(HttpStatus.BAD_REQUEST, code, detail);
    }

    public static ProblemException unauthorized(String code, String detail) {
        return new ProblemException(HttpStatus.UNAUTHORIZED, code, detail);
    }

    public static ProblemException forbidden(String code, String detail) {
        return new ProblemException(HttpStatus.FORBIDDEN, code, detail);
    }

    public static ProblemException notFound(String code, String detail) {
        return new ProblemException(HttpStatus.NOT_FOUND, code, detail);
    }

    public static ProblemException conflict(String code, String detail) {
        return new ProblemException(HttpStatus.CONFLICT, code, detail);
    }
}
