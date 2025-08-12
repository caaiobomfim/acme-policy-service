package com.acme.insurance.policy.application;

import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.app.error.PolicyCancelConflictException;
import com.acme.insurance.policy.app.error.PolicyNotFoundException;
import com.acme.insurance.policy.app.mapper.ApiPolicyMapper;
import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelPolicyServiceTest {

    @Mock
    PolicyRepository policyRepository;

    @Mock
    PolicyStateMachine policyStateMachine;

    @Mock
    ApiPolicyMapper apiPolicyMapper;

    @InjectMocks
    CancelPolicyService service;

    @Test
    @DisplayName("Happy path: encontra, cancela, reconsulta e mapeia a própria 'updated' quando findById(updated) vazio")
    void execute_happyPath_findByIdEmpty() {
        UUID id = UUID.randomUUID();

        Policy found = mock(Policy.class);
        when(found.isFinalStatus()).thenReturn(false);

        when(policyRepository.findById(id)).thenReturn(Optional.of(found), Optional.empty());

        Policy updated = mock(Policy.class);
        when(updated.id()).thenReturn(id);
        when(updated.status()).thenReturn(PolicyStatus.CANCELLED);
        when(policyStateMachine.cancel(same(found), eq("BY_CUSTOMER_REQUEST"))).thenReturn(updated);

        ArgumentCaptor<Policy> toResponseCap = ArgumentCaptor.forClass(Policy.class);
        when(apiPolicyMapper.toResponse(toResponseCap.capture()))
                .thenReturn(mock(PolicyResponseDto.class));

        var out = service.execute(id);
        assertThat(out).isNotNull();
        assertThat(toResponseCap.getValue()).isSameAs(updated);

        InOrder inOrder = inOrder(policyRepository, policyStateMachine, apiPolicyMapper);
        inOrder.verify(policyRepository).findById(id);
        inOrder.verify(policyStateMachine).cancel(found, "BY_CUSTOMER_REQUEST");
        inOrder.verify(policyRepository).findById(id);
        inOrder.verify(apiPolicyMapper).toResponse(updated);
    }

    @Test
    @DisplayName("Quando findById(updated) retorna presente, mapeia a policy persistida (pós-cancelamento)")
    void execute_usesPersistedFromRepository() {
        UUID id = UUID.randomUUID();

        Policy found = mock(Policy.class);
        when(found.isFinalStatus()).thenReturn(false);

        Policy persisted = mock(Policy.class);
        when(persisted.id()).thenReturn(id);
        when(persisted.status()).thenReturn(PolicyStatus.CANCELLED);

        when(policyRepository.findById(id)).thenReturn(Optional.of(found), Optional.of(persisted));

        Policy updated = mock(Policy.class);
        when(updated.id()).thenReturn(id);
        when(policyStateMachine.cancel(same(found), eq("BY_CUSTOMER_REQUEST"))).thenReturn(updated);

        ArgumentCaptor<Policy> toResponseCap = ArgumentCaptor.forClass(Policy.class);
        when(apiPolicyMapper.toResponse(toResponseCap.capture()))
                .thenReturn(mock(PolicyResponseDto.class));

        var out = service.execute(id);
        assertThat(out).isNotNull();
        assertThat(toResponseCap.getValue()).isSameAs(persisted);

        verify(policyRepository, times(2)).findById(id);
        verify(apiPolicyMapper).toResponse(persisted);
    }

    @Test
    @DisplayName("Not found: quando a policy não existe, lança PolicyNotFoundException e não chama FSM/mapper")
    void execute_notFound_throwsPolicyNotFound() {
        UUID id = UUID.randomUUID();
        when(policyRepository.findById(id)).thenReturn(Optional.empty());

        PolicyNotFoundException ex =
                catchThrowableOfType(() -> service.execute(id), PolicyNotFoundException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
        verifyNoInteractions(policyStateMachine, apiPolicyMapper);
    }

    @Test
    @DisplayName("Status final: lança PolicyCancelConflictException (409) e não chama FSM nem mapper")
    void execute_finalStatus_throwsConflict() {
        UUID id = UUID.randomUUID();

        Policy found = mock(Policy.class);
        when(found.isFinalStatus()).thenReturn(true);
        when(found.status()).thenReturn(PolicyStatus.CANCELLED);
        when(found.id()).thenReturn(id);
        when(policyRepository.findById(id)).thenReturn(Optional.of(found));

        PolicyCancelConflictException ex =
                catchThrowableOfType(() -> service.execute(id), PolicyCancelConflictException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());

        verify(policyStateMachine, never()).cancel(any(), anyString());
        verify(apiPolicyMapper, never()).toResponse(any());
    }
}