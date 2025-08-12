package com.acme.insurance.policy.app.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalProblemHandler {

    private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;

    private final ObjectMapper objectMapper;

    public GlobalProblemHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> handle(ResponseStatusException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(ex.getStatusCode());
        pd.setTitle(Optional.ofNullable(ex.getReason()).orElse("Not Found"));
        pd.setDetail(Optional.ofNullable(ex.getReason()).orElse("Resource not found"));
        pd.setType(URI.create("https://api.acme.com/errors/policy-not-found"));
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", MDC.get("traceId"));
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handle(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        pd.setType(URI.create("https://acme.example/errors/validation"));
        pd.setDetail("One or more fields are invalid");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", toJsonFieldPath(fe.getField()),
                        "message", Objects.requireNonNull(fe.getDefaultMessage())
                ))
                .toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM).body(pd);
    }

    @ExceptionHandler({ MethodArgumentTypeMismatchException.class, ConstraintViolationException.class })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        pd.setType(URI.create("https://acme.example/errors/bad-request"));
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM).body(pd);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ProblemDetail> handleSpringError(ErrorResponseException ex, HttpServletRequest req) {
        ProblemDetail pd = ex.getBody();
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(ex.getStatusCode()).contentType(PROBLEM).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
        pd.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        pd.setType(URI.create("https://acme.example/errors/internal"));
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(PROBLEM).body(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        pd.setType(URI.create("https://acme.example/errors/invalid-format"));
        pd.setDetail("Malformed or invalid request body");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now());

        if (ex.getCause() instanceof InvalidFormatException ife) {
            var errors = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .map(field -> Map.of(
                            "field", field,
                            "rejectedValue", String.valueOf(ife.getValue()),
                            "expectedType", ife.getTargetType().getSimpleName()
                    ))
                    .toList();
            if (!errors.isEmpty()) {
                pd.setProperty("errors", errors);
                pd.setDetail("One or more fields have invalid format");
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    private String toJsonFieldPath(String javaPath) {
        PropertyNamingStrategy s =
                objectMapper.getSerializationConfig().getPropertyNamingStrategy();

        PropertyNamingStrategies.NamingBase base = (s instanceof PropertyNamingStrategies.NamingBase)
                ? (PropertyNamingStrategies.NamingBase) s
                : (PropertyNamingStrategies.NamingBase) PropertyNamingStrategies.SNAKE_CASE;

        String[] parts = javaPath.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = translateSegment(parts[i], base);
        }
        return String.join(".", parts);
    }

    private static final Pattern SEGMENT = Pattern.compile("^([A-Za-z0-9_]+)(.*)$");

    private String translateSegment(String segment, PropertyNamingStrategies.NamingBase base) {
        Matcher m = SEGMENT.matcher(segment);
        if (!m.matches()) return segment;
        String prop = m.group(1);
        String rest = m.group(2);
        return base.translate(prop) + rest;
    }
}