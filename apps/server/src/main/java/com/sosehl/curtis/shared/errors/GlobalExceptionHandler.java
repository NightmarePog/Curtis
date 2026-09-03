package com.sosehl.curtis.shared.errors;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(
        GlobalExceptionHandler.class
    );
    private static final String ERROR_TYPE_PREFIX = "urn:curtis:error:";

    private final Tracer tracer;

    public GlobalExceptionHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception
            .getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                fieldErrors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
                )
            );
        ProblemDetail problem = exception.updateAndGetBody(
            getMessageSource(),
            Locale.ENGLISH
        );
        problem.setProperty("fieldErrors", fieldErrors);
        return handleExceptionInternal(
            exception,
            problem,
            headers,
            status,
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
        HandlerMethodValidationException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getParameterValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            String field = parameter == null ? "request" : parameter;
            result.getResolvableErrors().forEach(error ->
                fieldErrors.putIfAbsent(field, error.getDefaultMessage())
            );
        });
        ProblemDetail problem = exception.updateAndGetBody(
            getMessageSource(),
            Locale.ENGLISH
        );
        problem.setProperty("fieldErrors", fieldErrors);
        return handleExceptionInternal(
            exception,
            problem,
            headers,
            status,
            request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> handleConstraintViolation(
        ConstraintViolationException exception,
        WebRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception
            .getConstraintViolations()
            .forEach(violation ->
                fieldErrors.put(
                    violation.getPropertyPath().toString(),
                    violation.getMessage()
                )
            );
        return response(
            HttpStatus.BAD_REQUEST,
            "validation_failed",
            "The request contains invalid fields.",
            Map.of("fieldErrors", fieldErrors),
            request
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<Object> handleOptimisticLock(WebRequest request) {
        return response(
            HttpStatus.CONFLICT,
            "version_conflict",
            "The resource changed since it was loaded.",
            Map.of(),
            request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> handleIntegrityViolation(
        DataIntegrityViolationException exception,
        WebRequest request
    ) {
        LOG.warn("Database constraint rejected a request", exception);
        return response(
            HttpStatus.CONFLICT,
            "data_conflict",
            "The requested change conflicts with existing data.",
            Map.of(),
            request
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(
        Exception exception,
        WebRequest request
    ) {
        LOG.error("Unhandled request failure", exception);
        return response(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "internal_error",
            "The request could not be completed.",
            Map.of(),
            request
        );
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
        @Nullable Object body,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        if (body instanceof ProblemDetail problem) {
            addCurtisCode(problem);
            addTraceId(problem);
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.addAll(headers);
        responseHeaders.setContentLanguage(Locale.ENGLISH);
        return super.createResponseEntity(
            body,
            responseHeaders,
            status,
            request
        );
    }

    private ResponseEntity<Object> response(
        HttpStatus status,
        String code,
        String detail,
        Map<String, Object> properties,
        WebRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            status,
            detail
        );
        problem.setType(URI.create(ERROR_TYPE_PREFIX + code));
        properties.forEach(problem::setProperty);
        return createResponseEntity(
            problem,
            HttpHeaders.EMPTY,
            status,
            request
        );
    }

    private void addCurtisCode(ProblemDetail problem) {
        String type = problem.getType().toString();
        if (type.startsWith(ERROR_TYPE_PREFIX)) {
            problem.setProperty(
                "code",
                type.substring(ERROR_TYPE_PREFIX.length())
            );
        }
    }

    private void addTraceId(ProblemDetail problem) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            problem.setProperty(
                "traceId",
                currentSpan.context().traceId()
            );
        }
    }
}
