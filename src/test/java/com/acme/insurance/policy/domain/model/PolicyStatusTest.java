package com.acme.insurance.policy.domain.model;

import com.acme.insurance.policy.domain.fraud.FraudClassification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;

import static com.acme.insurance.policy.domain.model.PolicyStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PolicyStatusTest {

    @Test
    @DisplayName("RECEIVED.onFraud: aprovado -> VALIDATED depois PENDING; não finaliza")
    void received_onFraud_approved() {
        var ctx = mock(PolicyStatus.TransitionContext.class);

        RECEIVED.onFraud(ctx, FraudClassification.REGULAR, "AUTO", new BigDecimal("1000"));

        InOrder in = inOrder(ctx);
        in.verify(ctx).moveTo(VALIDATED, "REGULAR");
        in.verify(ctx).moveTo(PENDING, "REGULAR");
        verify(ctx, never()).finish();
    }

    @Test
    @DisplayName("RECEIVED.onFraud: reprovado -> REJECTED e finish()")
    void received_onFraud_rejected() {
        var ctx = mock(TransitionContext.class);

        RECEIVED.onFraud(ctx, FraudClassification.HIGH_RISK, "AUTO", new BigDecimal("300000"));

        InOrder in = inOrder(ctx);
        in.verify(ctx).moveTo(REJECTED, "HIGH_RISK");
        in.verify(ctx).finish();

        verify(ctx, never()).moveTo(eq(VALIDATED), anyString());
        verify(ctx, never()).moveTo(eq(PENDING), anyString());
    }

    @Test
    @DisplayName("PENDING.onPaymentConfirmed: bothSignals=true -> APPROVED e finish()")
    void pending_onPaymentConfirmed_bothTrue() {
        var ctx = mock(TransitionContext.class);
        when(ctx.bothSignalsArrived()).thenReturn(true);

        PENDING.onPaymentConfirmed(ctx);

        InOrder in = inOrder(ctx);
        in.verify(ctx).bothSignalsArrived();
        in.verify(ctx).moveTo(APPROVED, "PAYMENT+SUBSCRIPTION");
        in.verify(ctx).finish();
    }

    @Test
    @DisplayName("PENDING.onPaymentConfirmed: bothSignals=false -> no-op")
    void pending_onPaymentConfirmed_bothFalse() {
        var ctx = mock(TransitionContext.class);
        when(ctx.bothSignalsArrived()).thenReturn(false);

        PENDING.onPaymentConfirmed(ctx);

        verify(ctx).bothSignalsArrived();
        verify(ctx, never()).moveTo(any(), anyString());
        verify(ctx, never()).finish();
    }

    @Test
    @DisplayName("PENDING.onSubscriptionAuthorized: bothSignals=true -> APPROVED e finish()")
    void pending_onSubscriptionAuthorized_bothTrue() {
        var ctx = mock(TransitionContext.class);
        when(ctx.bothSignalsArrived()).thenReturn(true);

        PENDING.onSubscriptionAuthorized(ctx);

        InOrder in = inOrder(ctx);
        in.verify(ctx).bothSignalsArrived();
        in.verify(ctx).moveTo(APPROVED, "PAYMENT+SUBSCRIPTION");
        in.verify(ctx).finish();
    }

    @Test
    @DisplayName("PENDING.onSubscriptionAuthorized: bothSignals=false -> no-op")
    void pending_onSubscriptionAuthorized_bothFalse() {
        var ctx = mock(TransitionContext.class);
        when(ctx.bothSignalsArrived()).thenReturn(false);

        PENDING.onSubscriptionAuthorized(ctx);

        verify(ctx).bothSignalsArrived();
        verify(ctx, never()).moveTo(any(), anyString());
        verify(ctx, never()).finish();
    }

    @Test
    @DisplayName("PENDING.onPaymentDenied: -> REJECTED (BY_PAYMENT) e finish()")
    void pending_onPaymentDenied() {
        var ctx = mock(TransitionContext.class);

        PENDING.onPaymentDenied(ctx);

        InOrder in = inOrder(ctx);
        in.verify(ctx).moveTo(REJECTED, "BY_PAYMENT");
        in.verify(ctx).finish();
    }

    @Test
    @DisplayName("PENDING.onSubscriptionDenied: -> REJECTED (BY_SUBSCRIPTION) e finish()")
    void pending_onSubscriptionDenied() {
        var ctx = mock(TransitionContext.class);

        PENDING.onSubscriptionDenied(ctx);

        InOrder in = inOrder(ctx);
        in.verify(ctx).moveTo(REJECTED, "BY_SUBSCRIPTION");
        in.verify(ctx).finish();
    }

    @Test
    @DisplayName("VALIDATED.onFraud: no-op (sem interações no ctx)")
    void validated_onFraud_noop() {
        var ctx = mock(TransitionContext.class);
        VALIDATED.onFraud(ctx, FraudClassification.REGULAR, "AUTO", new BigDecimal("1000"));
        verifyNoInteractions(ctx);
    }

    @Test
    @DisplayName("canTransitionTo: cobre exemplos permitidos e proibidos")
    void canTransitionTo_examples() {
        assertThat(RECEIVED.canTransitionTo(VALIDATED)).isTrue();
        assertThat(RECEIVED.canTransitionTo(CANCELLED)).isTrue();
        assertThat(RECEIVED.canTransitionTo(APPROVED)).isFalse();

        assertThat(VALIDATED.canTransitionTo(PENDING)).isTrue();
        assertThat(VALIDATED.canTransitionTo(REJECTED)).isTrue();
        assertThat(VALIDATED.canTransitionTo(APPROVED)).isFalse();

        assertThat(PENDING.canTransitionTo(APPROVED)).isTrue();
        assertThat(PENDING.canTransitionTo(CANCELLED)).isTrue();
        assertThat(PENDING.canTransitionTo(VALIDATED)).isFalse();
    }

    @Test
    @DisplayName("isFinal: APPROVED/REJECTED/CANCELLED = true; RECEIVED/VALIDATED/PENDING = false")
    void isFinal_cases() {
        assertThat(APPROVED.isFinal()).isTrue();
        assertThat(REJECTED.isFinal()).isTrue();
        assertThat(CANCELLED.isFinal()).isTrue();

        assertThat(RECEIVED.isFinal()).isFalse();
        assertThat(VALIDATED.isFinal()).isFalse();
        assertThat(PENDING.isFinal()).isFalse();
    }

    @Test
    @DisplayName("toJson/fromJson: case-insensitive e simétrico")
    void json_roundTrip() {
        assertThat(PolicyStatus.fromJson("approved")).isEqualTo(APPROVED);
        assertThat(PolicyStatus.fromJson("ApPrOvEd")).isEqualTo(APPROVED);
        assertThat(APPROVED.toJson()).isEqualTo("APPROVED");

        assertThatThrownBy(() -> PolicyStatus.fromJson("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}