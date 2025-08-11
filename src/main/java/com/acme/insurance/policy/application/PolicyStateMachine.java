package com.acme.insurance.policy.application;

import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import com.acme.insurance.policy.domain.events.PolicyRequestStatusChangedEvent;
import com.acme.insurance.policy.domain.fraud.FraudClassification;
import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import com.acme.insurance.policy.domain.model.PolicyStatus.TransitionContext;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import com.acme.insurance.policy.domain.ports.out.PolicyRequestPublisher;
import com.acme.insurance.policy.infra.memory.InMemoryCorrelationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
public class PolicyStateMachine {

    private static final Logger log = LoggerFactory.getLogger(PolicyStateMachine.class);

    private final PolicyRepository policyRepository;
    private final PolicyRequestPublisher policyRequestPublisher;
    private final InMemoryCorrelationStore inMemoryCorrelationStore;

    public PolicyStateMachine(PolicyRepository policyRepository,
                              PolicyRequestPublisher policyRequestPublisher,
                              InMemoryCorrelationStore inMemoryCorrelationStore) {
        this.policyRepository = policyRepository;
        this.policyRequestPublisher = policyRequestPublisher;
        this.inMemoryCorrelationStore = inMemoryCorrelationStore;
    }

    private TransitionContext ctx(Policy policy) {
        return new TransitionContext() {
            private Policy current = policy;

            @Override
            public void moveTo(PolicyStatus next, String reason) {
                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                log.info("[FSM] {} -> {} (reason={}) id={}", current.status(), next, reason, current.id());

                if (!current.status().canTransitionTo(next) && !next.isFinal()) {
                    log.warn("[FSM] Transição não permitida {} -> {} para id={}", current.status(), next, current.id());
                }

                current = current.withStatusAndHistory(next, now);
                policyRepository.save(current);
                policyRequestPublisher.publish(new PolicyRequestStatusChangedEvent(
                        current.id(),
                        current.customerId(),
                        current.productId(),
                        current.status().name(),
                        reason,
                        now.toInstant()
                ));

                if (current.isFinalStatus()) {
                    inMemoryCorrelationStore.clear(current.id());
                }
            }

            @Override
            public void finish() {
                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                current = current.withFinishedAt(now);
                policyRepository.save(current);
                log.info("[FSM] finishedAt set id={} at={}", current.id(), now);
            }

            @Override
            public boolean bothSignalsArrived() {
                boolean ok = inMemoryCorrelationStore.bothDone(current.id());
                log.debug("[FSM] bothSignalsArrived? {} id={}", ok, current.id());
                return ok;
            }
        };
    }

    public void onFraud(Policy policy, FraudClassification classification, String category, BigDecimal insuredAmount) {
        log.info("[FSM] onFraud id={} status={} classification={}", policy.id(), policy.status(), classification);
        policy.status().onFraud(ctx(policy), classification, category, insuredAmount);
    }

    public void onPaymentConfirmed(Policy policy) {
        log.info("[FSM] onPaymentConfirmed id={} status={}", policy.id(), policy.status());
        policy.status().onPaymentConfirmed(ctx(policy));
    }

    public void onPaymentDenied(Policy policy) {
        log.info("[FSM] onPaymentDenied id={} status={}", policy.id(), policy.status());
        policy.status().onPaymentDenied(ctx(policy));
    }

    public void onSubscriptionAuthorized(Policy policy) {
        log.info("[FSM] onSubscriptionAuthorized id={} status={}", policy.id(), policy.status());
        policy.status().onSubscriptionAuthorized(ctx(policy));
    }

    public void onSubscriptionDenied(Policy policy) {
        log.info("[FSM] onSubscriptionDenied id={} status={}", policy.id(), policy.status());
        policy.status().onSubscriptionDenied(ctx(policy));
    }

    public Policy cancel(Policy policy, String reason) {
        log.info("[FSM] cancel id={} status={} reason={}", policy.id(), policy.status(), reason);
        TransitionContext c = ctx(policy);
        c.moveTo(PolicyStatus.CANCELLED, reason);
        c.finish();
        return policyRepository.findById(policy.id()).orElseThrow();
    }

    public void publishCreated(Policy policy) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        policyRequestPublisher.publish(new PolicyRequestCreatedEvent(
                policy.id(),
                policy.customerId(),
                policy.productId(),
                policy.status().name(),
                now.toInstant()
        ));
    }

    public Policy reload(UUID id) {
        return policyRepository.findById(id).orElseThrow();
    }
}