package com.acme.insurance.policy.app.controller;

import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.domain.ports.in.CancelPolicyUseCase;
import com.acme.insurance.policy.domain.ports.in.CreatePolicyUseCase;
import com.acme.insurance.policy.domain.ports.in.GetPolicyByIdQuery;
import com.acme.insurance.policy.domain.ports.in.ListPoliciesByCustomerQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyControllerTest {

    @Mock
    private CreatePolicyUseCase createPolicyUseCase;

    @Mock
    private GetPolicyByIdQuery getPolicyByIdQuery;

    @Mock
    private ListPoliciesByCustomerQuery listPoliciesByCustomerQuery;

    @Mock
    private CancelPolicyUseCase cancelPolicyUseCase;

    @InjectMocks
    private PolicyController controller;

    @BeforeEach
    void setup() {}

    @Test
    @DisplayName("GET /policies/{id} -> 200 com body (mesma instância)")
    void getById_ok() {
        UUID id = UUID.randomUUID();
        var dto = sampleResponseDto(UUID.randomUUID(), "APPROVED");
        when(getPolicyByIdQuery.execute(id)).thenReturn(dto);

        ResponseEntity<PolicyResponseDto> rsp = controller.get(id);

        assertThat(rsp.getStatusCode().value()).isEqualTo(200);
        assertThat(rsp.getBody()).isSameAs(dto);

        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(getPolicyByIdQuery).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(id);
    }

    @Test
    @DisplayName("GET /policies?customerId=... -> 200 com lista")
    void listByCustomer_ok() {
        UUID customerId = UUID.randomUUID();
        var one = sampleResponseDto(UUID.randomUUID(), "PENDING");
        var two = sampleResponseDto(UUID.randomUUID(), "CANCELLED");
        when(listPoliciesByCustomerQuery.execute(customerId)).thenReturn(List.of(one, two));

        ResponseEntity<List<PolicyResponseDto>> rsp = controller.list(customerId);

        assertThat(rsp.getStatusCode().value()).isEqualTo(200);
        assertThat(rsp.getBody()).isNotNull().hasSize(2);
        verify(listPoliciesByCustomerQuery).execute(customerId);
    }

    @Test
    @DisplayName("PATCH /policies/{id}/cancel -> 204 sem body")
    void cancel_ok() {
        UUID id = UUID.randomUUID();
        when(cancelPolicyUseCase.execute(id)).thenReturn(sampleResponseDto(id, "CANCELLED"));

        ResponseEntity<PolicyResponseDto> rsp = controller.cancel(id);

        assertThat(rsp.getStatusCode().value()).isEqualTo(204);
        assertThat(rsp.getBody()).isNull();
        verify(cancelPolicyUseCase).execute(id);
    }

    @Test
    @DisplayName("POST /policies -> 201 com Location e body")
    void create_created() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/policies");
        request.setServerName("localhost");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        var req = sampleRequestDto();

        when(createPolicyUseCase.execute(any())).thenReturn(sampleResponseDto(UUID.randomUUID(), "PENDING"));

        ResponseEntity<PolicyResponseDto> rsp = controller.create(req);

        assertThat(rsp.getStatusCode().value()).isEqualTo(201);
        assertThat(rsp.getBody()).isNotNull();

        UUID idFromBody = getId(rsp.getBody());
        assertThat(idFromBody).isNotNull();

        URI location = rsp.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.toString()).endsWith("/policies/" + idFromBody);

        verify(createPolicyUseCase).execute(any());
    }

    private PolicyRequestDto sampleRequestDto() {
        try {
            Method b = PolicyRequestDto.class.getMethod("builder");
            Object builder = b.invoke(null);

            maybeSet(builder, "customerId", UUID.randomUUID());
            maybeSet(builder, "productId", UUID.randomUUID());
            maybeSet(builder, "category", "AUTO");
            maybeSet(builder, "salesChannel", "ONLINE");
            maybeSet(builder, "paymentMethod", "CREDIT_CARD");
            return (PolicyRequestDto) builder.getClass().getMethod("build").invoke(builder);
        } catch (Exception ignore) {

            for (Constructor<?> c : PolicyRequestDto.class.getConstructors()) {
                try {
                    Object[] args = defaultsFor(c.getParameterTypes());
                    return (PolicyRequestDto) c.newInstance(args);
                } catch (Exception ignored) {

                }
            }
            throw new IllegalStateException("Ajuste sampleRequestDto() para o formato real do seu PolicyRequestDto.");
        }
    }

    private PolicyResponseDto sampleResponseDto(UUID id, String status) {
        try {

            Method b = PolicyResponseDto.class.getMethod("builder");
            Object builder = b.invoke(null);
            maybeSet(builder, "id", id);
            maybeSet(builder, "status", status);
            maybeSet(builder, "customerId", UUID.randomUUID());
            maybeSet(builder, "createdAt", OffsetDateTime.now());
            return (PolicyResponseDto) builder.getClass().getMethod("build").invoke(builder);
        } catch (Exception ignore) {

            for (Constructor<?> c : PolicyResponseDto.class.getConstructors()) {
                try {
                    Object[] args = defaultsFor(c.getParameterTypes());

                    PolicyResponseDto dto = (PolicyResponseDto) c.newInstance(args);
                    maybeSet(dto, "setId", id);
                    maybeSet(dto, "setStatus", status);
                    return dto;
                } catch (Exception ignored) {

                }
            }
            throw new IllegalStateException("Ajuste sampleResponseDto() para o formato real do seu PolicyResponseDto.");
        }
    }

    private void maybeSet(Object target, String methodName, Object value) {
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            Class<?>[] pts = m.getParameterTypes();
            if (pts.length == 1 && pts[0].isAssignableFrom(value.getClass())) {
                try { m.invoke(target, value); } catch (Exception ignored) {}
                break;
            }
        }
    }

    private Object[] defaultsFor(Class<?>[] types) {
        List<Object> out = new ArrayList<>(types.length);
        for (Class<?> t : types) {
            if (t.equals(UUID.class)) out.add(UUID.randomUUID());
            else if (t.equals(String.class)) out.add("dummy");
            else if (t.equals(OffsetDateTime.class)) out.add(OffsetDateTime.now());
            else if (t.isPrimitive()) out.add(primitiveDefault(t));
            else out.add(null);
        }
        return out.toArray();
    }

    private Object primitiveDefault(Class<?> t) {
        if (t == boolean.class) return false;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        if (t == double.class) return 0d;
        if (t == float.class) return 0f;
        if (t == short.class) return (short)0;
        if (t == byte.class) return (byte)0;
        if (t == char.class) return '\0';
        return null;
    }

    private UUID getId(PolicyResponseDto dto) {
        try { return (UUID) dto.getClass().getMethod("id").invoke(dto); } catch (Exception ignored) {}
        try { return (UUID) dto.getClass().getMethod("getId").invoke(dto); } catch (Exception ignored) {}
        return null;
    }
    private String getStatus(PolicyResponseDto dto) {
        try { return (String) dto.getClass().getMethod("status").invoke(dto); } catch (Exception ignored) {}
        try { return (String) dto.getClass().getMethod("getStatus").invoke(dto); } catch (Exception ignored) {}
        return null;
    }
}
