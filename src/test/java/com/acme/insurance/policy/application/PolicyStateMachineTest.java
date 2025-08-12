package com.acme.insurance.policy.application;

import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import com.acme.insurance.policy.domain.events.PolicyRequestStatusChangedEvent;
import com.acme.insurance.policy.domain.fraud.FraudClassification;
import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import com.acme.insurance.policy.domain.ports.out.PolicyRequestPublisher;
import com.acme.insurance.policy.infra.memory.InMemoryCorrelationStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyStateMachineTest {

    @Mock
    PolicyRepository policyRepository;

    @Mock
    PolicyRequestPublisher policyRequestPublisher;

    @Mock
    InMemoryCorrelationStore inMemoryCorrelationStore;

    @InjectMocks
    PolicyStateMachine fsm;

    @Test
    @DisplayName("cancel(): moveTo(CANCELLED) publica evento, limpa store se final, faz finish() e retorna a persistida")
    void cancel_happyPath() {
        UUID id = UUID.randomUUID();

        Policy found = mock(Policy.class);
        when(found.id()).thenReturn(id);
        when(found.status()).thenReturn(PolicyStatus.APPROVED);

        Policy afterMove = mock(Policy.class);
        when(afterMove.id()).thenReturn(id);
        when(afterMove.status()).thenReturn(PolicyStatus.CANCELLED);
        when(afterMove.isFinalStatus()).thenReturn(true);

        Policy afterFinish = mock(Policy.class);
        when(afterFinish.id()).thenReturn(id);

        when(found.withStatusAndHistory(eq(PolicyStatus.CANCELLED), any()))
                .thenReturn(afterMove);
        when(afterMove.withFinishedAt(any()))
                .thenReturn(afterFinish);

        when(policyRepository.findById(id)).thenReturn(Optional.of(afterFinish));

        ArgumentCaptor<PolicyRequestStatusChangedEvent> evtCap =
                ArgumentCaptor.forClass(PolicyRequestStatusChangedEvent.class);

        Policy out = fsm.cancel(found, "BY_CUSTOMER_REQUEST");

        assertThat(out).isSameAs(afterFinish);

        InOrder inOrder = inOrder(policyRepository, policyRequestPublisher, inMemoryCorrelationStore);
        inOrder.verify(policyRepository).save(same(afterMove));
        inOrder.verify(policyRequestPublisher).publish(evtCap.capture());
        inOrder.verify(inMemoryCorrelationStore).clear(eq(id));
        inOrder.verify(policyRepository).save(same(afterFinish));
        inOrder.verify(policyRepository).findById(id);

        PolicyRequestStatusChangedEvent evt = evtCap.getValue();
        assertThat(evt.requestId()).isEqualTo(id);
        assertThat(evt.status()).isEqualTo("CANCELLED");
        assertThat(evt.occurredAt()).isNotNull();

        verifyNoMoreInteractions(inMemoryCorrelationStore);
    }


    @Test
    @DisplayName("publishCreated(): publica PolicyRequestCreatedEvent com dados da policy")
    void publishCreated_publishesEvent() {
        UUID id = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        UUID product = UUID.randomUUID();

        Policy policy = mock(Policy.class);
        when(policy.id()).thenReturn(id);
        when(policy.customerId()).thenReturn(customer);
        when(policy.productId()).thenReturn(product);
        when(policy.status()).thenReturn(PolicyStatus.RECEIVED);

        ArgumentCaptor<PolicyRequestCreatedEvent> evtCap =
                ArgumentCaptor.forClass(PolicyRequestCreatedEvent.class);

        fsm.publishCreated(policy);

        verify(policyRequestPublisher).publish(evtCap.capture());
        PolicyRequestCreatedEvent evt = evtCap.getValue();
        assertThat(evt.requestId()).isEqualTo(id);
        assertThat(evt.customerId()).isEqualTo(customer);
        assertThat(evt.productId()).isEqualTo(product);
        assertThat(evt.status()).isEqualTo("RECEIVED");
        assertThat(evt.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("reload(): devolve a policy do repositório quando encontrada")
    void reload_found() {
        UUID id = UUID.randomUUID();
        Policy persisted = mock(Policy.class);
        when(policyRepository.findById(id)).thenReturn(Optional.of(persisted));

        Policy out = fsm.reload(id);

        assertThat(out).isSameAs(persisted);
        verify(policyRepository).findById(id);
    }

    @Test
    @DisplayName("reload(): lança NoSuchElementException quando não encontrada")
    void reload_notFound() {
        UUID id = UUID.randomUUID();
        when(policyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fsm.reload(id))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("onFraud(): com status final, não há side-effects (repo/publisher/store)")
    void onFraud_finalStatus_noSideEffects() {
        UUID id = UUID.randomUUID();
        Policy policy = policyWithStatus(id, PolicyStatus.CANCELLED);

        fsm.onFraud(policy, FraudClassification.REGULAR, "AUTO", new BigDecimal("1000"));

        verifyNoInteractions(policyRepository, policyRequestPublisher, inMemoryCorrelationStore);
    }

    @Test
    @DisplayName("onPaymentConfirmed(): com status final, não há side-effects")
    void onPaymentConfirmed_finalStatus_noSideEffects() {
        UUID id = UUID.randomUUID();
        Policy policy = policyWithStatus(id, PolicyStatus.CANCELLED);

        fsm.onPaymentConfirmed(policy);

        verifyNoInteractions(policyRepository, policyRequestPublisher, inMemoryCorrelationStore);
    }

    @Test
    @DisplayName("onPaymentDenied(): com status final, não há side-effects")
    void onPaymentDenied_finalStatus_noSideEffects() {
        UUID id = UUID.randomUUID();
        Policy policy = policyWithStatus(id, PolicyStatus.CANCELLED);

        fsm.onPaymentDenied(policy);

        verifyNoInteractions(policyRepository, policyRequestPublisher, inMemoryCorrelationStore);
    }

    @Test
    @DisplayName("onSubscriptionAuthorized(): com status final, não há side-effects")
    void onSubscriptionAuthorized_finalStatus_noSideEffects() {
        UUID id = UUID.randomUUID();
        Policy policy = policyWithStatus(id, PolicyStatus.CANCELLED);

        fsm.onSubscriptionAuthorized(policy);

        verifyNoInteractions(policyRepository, policyRequestPublisher, inMemoryCorrelationStore);
    }

    @Test
    @DisplayName("onSubscriptionDenied(): com status final, não há side-effects")
    void onSubscriptionDenied_finalStatus_noSideEffects() {
        UUID id = UUID.randomUUID();
        Policy policy = policyWithStatus(id, PolicyStatus.CANCELLED);

        fsm.onSubscriptionDenied(policy);

        verifyNoInteractions(policyRepository, policyRequestPublisher, inMemoryCorrelationStore);
    }

    @Test
    @DisplayName("ctx.bothSignalsArrived(): delega no store e retorna false")
    void bothSignalsArrived_false() throws Exception {
        UUID id = UUID.randomUUID();
        Policy policy = mock(Policy.class);
        when(policy.id()).thenReturn(id);

        when(inMemoryCorrelationStore.bothDone(id)).thenReturn(false);

        PolicyStatus.TransitionContext ctx = ctxFor(policy);
        boolean out = ctx.bothSignalsArrived();

        assertThat(out).isFalse();
        verify(inMemoryCorrelationStore).bothDone(id);
        verifyNoInteractions(policyRepository, policyRequestPublisher);
    }

    @Test
    @DisplayName("ctx.bothSignalsArrived(): delega no store e retorna true")
    void bothSignalsArrived_true() throws Exception {
        UUID id = UUID.randomUUID();
        Policy policy = mock(Policy.class);
        when(policy.id()).thenReturn(id);

        when(inMemoryCorrelationStore.bothDone(id)).thenReturn(true);

        PolicyStatus.TransitionContext ctx = ctxFor(policy);
        boolean out = ctx.bothSignalsArrived();

        assertThat(out).isTrue();
        verify(inMemoryCorrelationStore).bothDone(id);
        verifyNoInteractions(policyRepository, policyRequestPublisher);
    }

    @Test
    @DisplayName("PENDING + bothSignals=true em onPaymentConfirmed -> APPROVED, finish e clear")
    void pending_paymentConfirmed_bothTrue() {
        UUID id = UUID.randomUUID();

        Policy pending = mock(Policy.class);
        when(pending.id()).thenReturn(id);
        when(pending.status()).thenReturn(PolicyStatus.PENDING);

        Policy approved = mock(Policy.class);
        when(approved.id()).thenReturn(id);
        when(approved.status()).thenReturn(PolicyStatus.APPROVED);
        when(approved.isFinalStatus()).thenReturn(true);

        Policy finished = mock(Policy.class);
        when(finished.id()).thenReturn(id);

        when(pending.withStatusAndHistory(eq(PolicyStatus.APPROVED), any()))
                .thenReturn(approved);
        when(approved.withFinishedAt(any()))
                .thenReturn(finished);

        when(inMemoryCorrelationStore.bothDone(id)).thenReturn(true);

        fsm.onPaymentConfirmed(pending);

        InOrder in = inOrder(inMemoryCorrelationStore, policyRepository, policyRequestPublisher);
        in.verify(inMemoryCorrelationStore).bothDone(id);
        in.verify(policyRepository).save(same(approved));
        in.verify(policyRequestPublisher).publish(any(PolicyRequestStatusChangedEvent.class));
        in.verify(inMemoryCorrelationStore).clear(id);
        in.verify(policyRepository).save(same(finished));
    }

    @Test
    @DisplayName("PENDING + bothSignals=false em onSubscriptionAuthorized -> no-op")
    void pending_subscriptionAuthorized_bothFalse() {
        UUID id = UUID.randomUUID();
        Policy pending = mock(Policy.class);
        when(pending.id()).thenReturn(id);
        when(pending.status()).thenReturn(PolicyStatus.PENDING);

        when(inMemoryCorrelationStore.bothDone(id)).thenReturn(false);

        fsm.onSubscriptionAuthorized(pending);

        verify(inMemoryCorrelationStore).bothDone(id);
        verifyNoInteractions(policyRepository, policyRequestPublisher);
    }

    private Policy policyWithStatus(UUID id, PolicyStatus status) {
        Policy p = mock(Policy.class);
        when(p.id()).thenReturn(id);
        when(p.status()).thenReturn(status);
        return p;
    }

    private PolicyStatus.TransitionContext ctxFor(Policy p) throws Exception {
        Method m = PolicyStateMachine.class.getDeclaredMethod("ctx", Policy.class);
        m.setAccessible(true);
        return (PolicyStatus.TransitionContext) m.invoke(fsm, p);
    }
}