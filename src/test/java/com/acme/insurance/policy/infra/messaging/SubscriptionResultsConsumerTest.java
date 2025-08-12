package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.app.dto.integration.SubscriptionResultEvent;
import com.acme.insurance.policy.application.PolicyStateMachine;
import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import com.acme.insurance.policy.infra.memory.InMemoryCorrelationStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionResultsConsumerTest {

    @Mock
    PolicyRepository policyRepository;

    @Mock
    PolicyStateMachine policyStateMachine;

    @Mock
    InMemoryCorrelationStore inMemoryCorrelationStore;

    @InjectMocks
    SubscriptionResultsConsumer consumer;

    private SubscriptionResultEvent evt(UUID id, String status) {
        var e = mock(SubscriptionResultEvent.class);
        when(e.requestId()).thenReturn(id);
        when(e.status()).thenReturn(status);
        return e;
    }

    private Policy policy(boolean finalStatus) {
        var p = mock(Policy.class);
        when(p.isFinalStatus()).thenReturn(finalStatus);
        return p;
    }

    @Test
    @DisplayName("DENIED: chama FSM.onSubscriptionDenied e não marca/limpa store")
    void denied_callsFsmDenied_only() {
        UUID id = UUID.randomUUID();
        var e = evt(id, "DENIED");
        var p = policy(false);

        when(policyRepository.findById(id)).thenReturn(Optional.of(p));

        consumer.onSubscription(e);

        verify(policyStateMachine).onSubscriptionDenied(p);
        verify(policyStateMachine, never()).onSubscriptionAuthorized(any());
        verify(inMemoryCorrelationStore, never()).markSubscription(any(), any());
        verify(inMemoryCorrelationStore, never()).clear(any());
    }

    @Test
    @DisplayName("AUTHORIZED: marca no store e chama FSM.onSubscriptionAuthorized")
    void authorized_marksAndCallsFsmAuthorized() {
        UUID id = UUID.randomUUID();
        Instant at = Instant.parse("2024-01-01T12:00:00Z");
        var e = evt(id, "AUTHORIZED");
        when(e.occurredAt()).thenReturn(at);

        var p = policy(false);
        when(policyRepository.findById(id)).thenReturn(Optional.of(p));

        consumer.onSubscription(e);

        verify(inMemoryCorrelationStore).markSubscription(id, at);
        verify(policyStateMachine).onSubscriptionAuthorized(p);
        verify(inMemoryCorrelationStore, never()).clear(any());
        verify(policyStateMachine, never()).onSubscriptionDenied(any());
    }

    @Test
    @DisplayName("Policy em estado final: apenas limpa store e não chama FSM")
    void finalStatus_clearsStore_only() {
        UUID id = UUID.randomUUID();
        var e = evt(id, "AUTHORIZED");
        var p = policy(true);
        when(p.status()).thenReturn(PolicyStatus.CANCELLED);

        when(policyRepository.findById(id)).thenReturn(Optional.of(p));

        consumer.onSubscription(e);

        verify(inMemoryCorrelationStore).clear(id);
        verify(inMemoryCorrelationStore, never()).markSubscription(any(), any());
        verify(policyStateMachine, never()).onSubscriptionAuthorized(any());
        verify(policyStateMachine, never()).onSubscriptionDenied(any());
    }

    @Test
    @DisplayName("Status desconhecido: ignora sem chamar FSM ou store")
    void unknownStatus_isIgnored() {
        UUID id = UUID.randomUUID();
        var e = evt(id, "SOMETHING_ELSE");
        var p = policy(false);

        when(policyRepository.findById(id)).thenReturn(Optional.of(p));

        consumer.onSubscription(e);

        verifyNoInteractions(policyStateMachine);
        verify(inMemoryCorrelationStore, never()).markSubscription(any(), any());
        verify(inMemoryCorrelationStore, never()).clear(any());
    }

    @Test
    @DisplayName("Policy não encontrada: ignora sem FSM/store")
    void policyNotFound_isIgnored() {
        UUID id = UUID.randomUUID();
        var e = evt(id, "AUTHORIZED");

        when(policyRepository.findById(id)).thenReturn(Optional.empty());

        consumer.onSubscription(e);

        verifyNoInteractions(policyStateMachine, inMemoryCorrelationStore);
    }
}