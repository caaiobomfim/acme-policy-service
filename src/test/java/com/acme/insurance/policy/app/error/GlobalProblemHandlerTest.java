package com.acme.insurance.policy.app.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalProblemHandlerTest {

    private GlobalProblemHandler handler;

    @BeforeEach
    void setUp() {
        ObjectMapper om = new ObjectMapper();
        om.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        handler = new GlobalProblemHandler(om);
    }

    private HttpServletRequest req(String path) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(path);
        return req;
    }
    static class Dummy {
        public void takeString(String v) {}
        public void takeInteger(Integer v) {}
    }

    @Test
    @DisplayName("ResponseStatusException -> ResponseEntity<ProblemDetail> com type/instance/traceId")
    void handle_responseStatusException() {
        String path = "/policies/123";
        HttpServletRequest req = req(path);

        MDC.put("traceId", "abc-123");
        try {
            var ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found");
            ResponseEntity<ProblemDetail> resp = handler.handle(ex, req);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

            ProblemDetail pd = resp.getBody();
            assertThat(pd).isNotNull();
            assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(pd.getTitle()).isEqualTo("Policy not found");
            assertThat(pd.getDetail()).isEqualTo("Policy not found");
            assertThat(pd.getType()).isEqualTo(URI.create("https://api.acme.com/errors/policy-not-found"));
            assertThat(pd.getInstance()).isEqualTo(URI.create(path));
            assertThat(pd.getProperties()).containsEntry("traceId", "abc-123");
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 com array de errors (field/message) em snake_case")
    void handle_methodArgumentNotValid() throws NoSuchMethodException {
        String path = "/policies";
        HttpServletRequest req = req(path);

        var target = new Object();
        var binding = new BeanPropertyBindingResult(target, "policyRequest");
        binding.addError(new FieldError("policyRequest", "customerId", "must not be null"));
        binding.addError(new FieldError("policyRequest", "category", "must not be blank"));

        Method m = Dummy.class.getMethod("takeString", String.class);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(new MethodParameter(m, 0), binding);

        ResponseEntity<ProblemDetail> resp = handler.handle(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail pd = resp.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getTitle()).isEqualTo("Bad Request");
        assertThat(pd.getType()).isEqualTo(URI.create("https://acme.example/errors/validation"));
        assertThat(pd.getDetail()).isEqualTo("One or more fields are invalid");
        assertThat(pd.getInstance()).isEqualTo(URI.create(path));
        assertThat(pd.getProperties()).containsKeys("timestamp", "errors");

        @SuppressWarnings("unchecked")
        var errors = (java.util.List<java.util.Map<String, String>>) pd.getProperties().get("errors");
        assertThat(errors).hasSize(2);
        assertThat(errors.get(0)).containsEntry("field", "customer_id").containsEntry("message", "must not be null");
        assertThat(errors.get(1)).containsEntry("field", "category").containsEntry("message", "must not be blank");
    }

    @Test
    @DisplayName("ConstraintViolationException -> 400 bad-request com timestamp")
    void handle_badRequest_constraintViolation() {
        String path = "/policies";
        HttpServletRequest req = req(path);

        var ex = new ConstraintViolationException("Invalid param", Set.of());

        ResponseEntity<ProblemDetail> resp = handler.handleBadRequest(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail pd = resp.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getTitle()).isEqualTo("Bad Request");
        assertThat(pd.getType()).isEqualTo(URI.create("https://acme.example/errors/bad-request"));
        assertThat(pd.getDetail()).contains("Invalid param");
        assertThat(pd.getInstance()).isEqualTo(URI.create(path));
        assertThat(pd.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException -> 400 bad-request")
    void handle_badRequest_typeMismatch() throws NoSuchMethodException {
        String path = "/policies/abc";
        HttpServletRequest req = req(path);

        Method m = Dummy.class.getMethod("takeInteger", Integer.class);
        var param = new MethodParameter(m, 0);
        var cause = new IllegalArgumentException("Failed to convert value");

        var ex = new MethodArgumentTypeMismatchException("abc", Integer.class, "id", param, cause);

        ResponseEntity<ProblemDetail> resp = handler.handleBadRequest(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail pd = resp.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getTitle()).isEqualTo("Bad Request");
        assertThat(pd.getType()).isEqualTo(URI.create("https://acme.example/errors/bad-request"));
        assertThat(pd.getInstance()).isEqualTo(URI.create(path));
        assertThat(pd.getProperties()).containsKey("timestamp");

        assertThat(pd.getDetail())
                .isNotBlank()
                .contains("Method parameter 'id'")
                .containsAnyOf("Integer", "java.lang.Integer");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException (InvalidFormat) -> 400 com errors[] e detail específico")
    void handle_notReadable_invalidFormat_withErrors() {
        String path = "/policies";
        HttpServletRequest req = req(path);

        InvalidFormatException ife = InvalidFormatException.from(null,
                "Cannot deserialize value", "abc", Integer.class);
        ife.prependPath(Object.class, "customer_id");

        var ex = new HttpMessageNotReadableException("JSON parse error", ife, null);

        ResponseEntity<ProblemDetail> resp = handler.handleNotReadable(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail pd = resp.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getTitle()).isEqualTo("Bad Request");
        assertThat(pd.getType()).isEqualTo(URI.create("https://acme.example/errors/invalid-format"));
        assertThat(pd.getDetail()).isEqualTo("One or more fields have invalid format");
        assertThat(pd.getInstance()).isEqualTo(URI.create(path));
        assertThat(pd.getProperties()).containsKey("timestamp");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> errors = (List<Map<String, String>>) pd.getProperties().get("errors");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0))
                .containsEntry("field", "customer_id")
                .containsEntry("rejectedValue", "abc")
                .containsEntry("expectedType", "Integer");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException (genérica) -> 400 sem errors[] e detail padrão")
    void handle_notReadable_generic_noErrors() {
        String path = "/policies";
        HttpServletRequest req = req(path);

        var ex = new HttpMessageNotReadableException("Empty body", (Throwable) null, null);

        ResponseEntity<ProblemDetail> resp = handler.handleNotReadable(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail pd = resp.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getTitle()).isEqualTo("Bad Request");
        assertThat(pd.getType()).isEqualTo(URI.create("https://acme.example/errors/invalid-format"));
        assertThat(pd.getDetail()).isEqualTo("Malformed or invalid request body");
        assertThat(pd.getInstance()).isEqualTo(URI.create(path));
        assertThat(pd.getProperties()).containsKey("timestamp");
        assertThat(pd.getProperties()).doesNotContainKey("errors");
    }

    @Test
    @DisplayName("ErrorResponseException -> usa body existente, adiciona instance/timestamp e status do erro")
    void handle_springError() {
        String path = "/policies/123";
        HttpServletRequest req = req(path);

        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflict!");
        body.setTitle("Conflict");
        body.setType(URI.create("https://acme.example/errors/conflict"));

        ErrorResponseException ex =
                new ErrorResponseException(HttpStatus.CONFLICT, body, null);

        ResponseEntity<ProblemDetail> resp = handler.handleSpringError(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail pd = resp.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getStatus()).isEqualTo(409);
        assertThat(pd.getTitle()).isEqualTo("Conflict");
        assertThat(pd.getDetail()).isEqualTo("Conflict!");
        assertThat(pd.getType()).isEqualTo(URI.create("https://acme.example/errors/conflict"));
        assertThat(pd.getInstance()).isEqualTo(URI.create(path));
        assertThat(pd.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Exception genérica -> 500 com type internal e mensagem padrão")
    void handle_generic() {
        String path = "/anything";
        HttpServletRequest req = req(path);

        ResponseEntity<ProblemDetail> resp = handler.handleGeneric(new RuntimeException("boom"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail pd = resp.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getStatus()).isEqualTo(500);
        assertThat(pd.getTitle()).isEqualTo("Internal Server Error");
        assertThat(pd.getDetail()).isEqualTo("Unexpected error");
        assertThat(pd.getType()).isEqualTo(URI.create("https://acme.example/errors/internal"));
        assertThat(pd.getInstance()).isEqualTo(URI.create(path));
        assertThat(pd.getProperties()).containsKey("timestamp");
    }
}