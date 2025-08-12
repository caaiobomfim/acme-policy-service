package com.acme.insurance.policy.application;

import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.app.error.PolicyNotFoundException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPolicyByIdServiceTest {

    @Mock
    PolicyRepository policyRepository;

    @Mock
    ApiPolicyMapper apiPolicyMapper;

    @InjectMocks
    GetPolicyByIdService service;

    @Test
    @DisplayName("Happy path: encontra, mapeia e retorna o DTO")
    void execute_found_mapsAndReturns() {
        UUID id = UUID.randomUUID();

        Policy domain = mock(Policy.class);
        when(policyRepository.findById(id)).thenReturn(Optional.of(domain));

        PolicyResponseDto dto = mock(PolicyResponseDto.class);
        when(apiPolicyMapper.toResponse(domain)).thenReturn(dto);

        PolicyResponseDto out = service.execute(id);

        assertThat(out).isSameAs(dto);

        InOrder inOrder = inOrder(policyRepository, apiPolicyMapper);
        inOrder.verify(policyRepository).findById(id);
        inOrder.verify(apiPolicyMapper).toResponse(domain);
    }

    @Test
    @DisplayName("Não encontrado: lança PolicyNotFoundException (404) com ProblemDetail preenchido e não chama o mapper")
    void execute_notFound_throwsPolicyNotFound() {
        UUID id = UUID.randomUUID();
        when(policyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(id))
                .isInstanceOf(PolicyNotFoundException.class)
                .satisfies(th -> {
                    PolicyNotFoundException ex = (PolicyNotFoundException) th;
                    assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
                    ProblemDetail body = ex.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getTitle()).isEqualTo("Not Found");
                    assertThat(body.getDetail()).isEqualTo("Policy not found");
                    assertThat(body.getType().toString()).isEqualTo("https://api.acme.com/errors/policy-not-found");
                    assertThat(body.getProperties()).containsEntry("policyId", id.toString());
                });

        verify(apiPolicyMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Exceção do repositório é propagada e mapper não é chamado")
    void execute_repoThrows_propagates() {
        UUID id = UUID.randomUUID();
        when(policyRepository.findById(id)).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.execute(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");

        verifyNoInteractions(apiPolicyMapper);
    }
}