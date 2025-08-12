package com.acme.insurance.policy.application;

import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.app.error.PolicyNotFoundByIdCustomerException;
import com.acme.insurance.policy.app.mapper.ApiPolicyMapper;
import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListPoliciesByCustomerServiceTest {

    @Mock
    PolicyRepository policyRepository;

    @Mock
    ApiPolicyMapper apiPolicyMapper;

    @InjectMocks
    ListPoliciesByCustomerService service;

    @Test
    @DisplayName("Happy path: consulta repo, mapeia cada Policy e preserva a ordem")
    void execute_happyPath_mapsAll_inOrder() {
        UUID customerId = UUID.randomUUID();

        Policy p1 = mock(Policy.class);
        Policy p2 = mock(Policy.class);
        when(policyRepository.findByCustomerId(eq(customerId)))
                .thenReturn(List.of(p1, p2));

        PolicyResponseDto r1 = mock(PolicyResponseDto.class);
        PolicyResponseDto r2 = mock(PolicyResponseDto.class);
        when(apiPolicyMapper.toResponse(p1)).thenReturn(r1);
        when(apiPolicyMapper.toResponse(p2)).thenReturn(r2);

        List<PolicyResponseDto> out = service.execute(customerId);

        assertThat(out).containsExactly(r1, r2);

        InOrder inOrder = inOrder(policyRepository, apiPolicyMapper);
        inOrder.verify(policyRepository).findByCustomerId(customerId);
        inOrder.verify(apiPolicyMapper).toResponse(p1);
        inOrder.verify(apiPolicyMapper).toResponse(p2);
    }

    @Test
    @DisplayName("Sem resultados: lança PolicyNotFoundByIdCustomerException (404) com ProblemDetail")
    void execute_emptyList_throwsProblem404() {
        UUID customerId = UUID.randomUUID();
        when(policyRepository.findByCustomerId(customerId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.execute(customerId))
                .isInstanceOf(PolicyNotFoundByIdCustomerException.class)
                .satisfies(th -> {
                    PolicyNotFoundByIdCustomerException ex = (PolicyNotFoundByIdCustomerException) th;
                    var pd = ex.getBody();
                    assertThat(pd.getStatus()).isEqualTo(404);
                    assertThat(pd.getTitle()).isEqualTo("Not Found");
                    assertThat(pd.getDetail()).contains("No policy was found for this customer");
                    assertThat(pd.getType().toString()).contains("customer-policies-not-found");
                    assertThat(Objects.requireNonNull(pd.getProperties()).get("customerId")).isEqualTo(customerId.toString());
                });

        verify(apiPolicyMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Exceção do repositório é propagada e mapper não é chamado")
    void execute_repoThrows_propagates() {
        UUID customerId = UUID.randomUUID();
        when(policyRepository.findByCustomerId(customerId))
                .thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.execute(customerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");

        verifyNoInteractions(apiPolicyMapper);
    }
}