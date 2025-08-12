package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.app.dto.integration.PaymentResultEvent;
import com.acme.insurance.policy.application.PolicyStateMachine;
import com.acme.insurance.policy.domain.model.Policy;
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
class PaymentResultsConsumerTest {

    @Mock
    PolicyRepository policyRepository;

    @Mock
    PolicyStateMachine policyStateMachine;

    @Mock
    InMemoryCorrelationStore inMemoryCorrelationStore;

    @InjectMocks
    PaymentResultsConsumer consumer;

    private PaymentResultEvent evt(UUID id, String status) {
        var e = mock(PaymentResultEvent.class);
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
    @DisplayName("DENIED: chama FSM.onPaymentDenied e não marca/limpa store")
    void denied_callsFsmDenied_only() {
        UUID id = UUID.randomUUID();
        var e = evt(id, "DENIED");
        var p = policy(false);

        when(policyRepository.findById(id)).thenReturn(Optional.of(p));

        consumer.onPayment(e);

        verify(policyStateMachine).onPaymentDenied(p);
        verify(policyStateMachine, never()).onPaymentConfirmed(any());
        verify(inMemoryCorrelationStore, never()).markPayment(any(), any());
        verify(inMemoryCorrelationStore, never()).clear(any());
    }

    @Test
    @DisplayName("CONFIRMED: marca no store e chama FSM.onPaymentConfirmed")
    void confirmed_marksAndCallsFsmConfirmed() {
        UUID id = UUID.randomUUID();
        Instant at = Instant.parse("2024-01-01T12:00:00Z");
        var e = evt(id, "CONFIRMED");
        when(e.occurredAt()).thenReturn(at);

        var p = policy(false);
        when(policyRepository.findById(id)).thenReturn(Optional.of(p));

        consumer.onPayment(e);

        verify(inMemoryCorrelationStore).markPayment(id, at);
        verify(policyStateMachine).onPaymentConfirmed(p);
        verify(inMemoryCorrelationStore, never()).clear(any());
        verify(policyStateMachine, never()).onPaymentDenied(any());
    }

    @Test
    @DisplayName("Policy em estado final: apenas limpa store e não chama FSM")
    void finalStatus_clearsStore_only() {
        UUID id = UUID.randomUUID();
        var e = evt(id, "CONFIRMED");
        var p = policy(true);

        when(policyRepository.findById(id)).thenReturn(Optional.of(p));

        consumer.onPayment(e);

        verify(inMemoryCorrelationStore).clear(id);
        verify(inMemoryCorrelationStore, never()).markPayment(any(), any());
        verify(policyStateMachine, never()).onPaymentConfirmed(any());
        verify(policyStateMachine, never()).onPaymentDenied(any());
    }

    @Test
    @DisplayName("Status desconhecido: ignora sem chamar FSM ou store")
    void unknownStatus_isIgnored() {
        UUID id = UUID.randomUUID();
        var e = evt(id, "WHATEVER");
        var p = policy(false);

        when(policyRepository.findById(id)).thenReturn(Optional.of(p));

        consumer.onPayment(e);

        verifyNoInteractions(policyStateMachine);
        verify(inMemoryCorrelationStore, never()).markPayment(any(), any());
        verify(inMemoryCorrelationStore, never()).clear(any());
    }

    @Test
    @DisplayName("Policy não encontrada: ignora sem FSM/store")
    void policyNotFound_isIgnored() {
        UUID id = UUID.randomUUID();
        var e = evt(id, "CONFIRMED");

        when(policyRepository.findById(id)).thenReturn(Optional.empty());

        consumer.onPayment(e);

        verifyNoInteractions(policyStateMachine, inMemoryCorrelationStore);
    }
}