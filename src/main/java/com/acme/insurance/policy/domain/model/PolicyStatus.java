package com.acme.insurance.policy.domain.model;

import com.acme.insurance.policy.domain.fraud.FraudClassification;
import com.acme.insurance.policy.domain.fraud.FraudRules;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum PolicyStatus {

    RECEIVED {
        @Override
        public void onFraud(TransitionContext ctx,
                            FraudClassification classification,
                            String category,
                            BigDecimal insuredAmount) {
            boolean approved = FraudRules.isApproved(classification, category, insuredAmount);
            if (approved) {
                ctx.moveTo(VALIDATED, classification.name());
                ctx.moveTo(PENDING,   classification.name());
            } else {
                ctx.moveTo(REJECTED,  classification.name());
                ctx.finish();
            }
        }
    },

    VALIDATED,

    PENDING {
        @Override public void onPaymentConfirmed(TransitionContext ctx) {
            if (ctx.bothSignalsArrived()) {
                ctx.moveTo(APPROVED, "PAYMENT+SUBSCRIPTION");
                ctx.finish();
            }
        }
        @Override public void onSubscriptionAuthorized(TransitionContext ctx) {
            if (ctx.bothSignalsArrived()) {
                ctx.moveTo(APPROVED, "PAYMENT+SUBSCRIPTION");
                ctx.finish();
            }
        }
        @Override public void onPaymentDenied(TransitionContext ctx) {
            ctx.moveTo(REJECTED, "BY_PAYMENT");
            ctx.finish();
        }
        @Override public void onSubscriptionDenied(TransitionContext ctx) {
            ctx.moveTo(REJECTED, "BY_SUBSCRIPTION");
            ctx.finish();
        }
    },

    APPROVED, REJECTED, CANCELLED;

    private static final boolean FAIL_ON_UNEXPECTED = false;

    public void onFraud(TransitionContext ctx,
                        FraudClassification classification,
                        String category,
                        BigDecimal insuredAmount) {
        if (FAIL_ON_UNEXPECTED) {
            throw new IllegalStateException("onFraud não permitido em " + this);
        }
    }

    public void onPaymentConfirmed(TransitionContext ctx) {
        if (FAIL_ON_UNEXPECTED) {
            throw new IllegalStateException("onPaymentConfirmed não permitido em " + this);
        }
    }

    public void onPaymentDenied(TransitionContext ctx) {
        if (FAIL_ON_UNEXPECTED) {
            throw new IllegalStateException("onPaymentDenied não permitido em " + this);
        }
    }

    public void onSubscriptionAuthorized(TransitionContext ctx) {
        if (FAIL_ON_UNEXPECTED) {
            throw new IllegalStateException("onSubscriptionAuthorized não permitido em " + this);
        }
    }

    public void onSubscriptionDenied(TransitionContext ctx) {
        if (FAIL_ON_UNEXPECTED) {
            throw new IllegalStateException("onSubscriptionDenied não permitido em " + this);
        }
    }

    private static final Map<PolicyStatus, Set<PolicyStatus>> ALLOWED = new EnumMap<>(Map.of(
            RECEIVED, EnumSet.of(VALIDATED, CANCELLED),
            VALIDATED, EnumSet.of(PENDING, REJECTED, CANCELLED),
            PENDING, EnumSet.of(APPROVED, REJECTED, CANCELLED)
    ));

    public boolean canTransitionTo(PolicyStatus next) {
        return ALLOWED.getOrDefault(this, EnumSet.noneOf(PolicyStatus.class)).contains(next);
    }

    public boolean isFinal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static PolicyStatus fromJson(String v) {
        return PolicyStatus.valueOf(v.toUpperCase());
    }

    public interface TransitionContext {
        void moveTo(PolicyStatus next, String reason);
        void finish();
        boolean bothSignalsArrived();
    }
}