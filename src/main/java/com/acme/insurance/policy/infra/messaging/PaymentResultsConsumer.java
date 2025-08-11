package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.app.dto.integration.PaymentResultEvent;
import com.acme.insurance.policy.application.PolicyStateMachine;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import com.acme.insurance.policy.infra.memory.InMemoryCorrelationStore;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.sqs.listeners.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentResultsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultsConsumer.class);

    private final PolicyRepository policyRepository;
    private final PolicyStateMachine policyStateMachine;
    private final InMemoryCorrelationStore inMemoryCorrelationStore;

    public PaymentResultsConsumer(PolicyRepository policyRepository,
                                  PolicyStateMachine policyStateMachine,
                                  InMemoryCorrelationStore inMemoryCorrelationStore) {
        this.policyRepository = policyRepository;
        this.policyStateMachine = policyStateMachine;
        this.inMemoryCorrelationStore = inMemoryCorrelationStore;
    }

    @SqsListener("${app.sqs.queues.payments}")
    public void onPayment(PaymentResultEvent e){
        log.info("[SQS] Mensagem recebida - requestId={} status={}", e.requestId(), e.status());

        policyRepository.findById(e.requestId()).ifPresentOrElse(policy -> {

            if (policy.isFinalStatus()) {
                log.info("Policy em estado final ({}). Ignorando evento de pagamento. id={}",
                        policy.status(), policy.id());
                inMemoryCorrelationStore.clear(e.requestId());
                return;
            }
            switch (e.status().toUpperCase()) {
                case "DENIED" -> {
                    log.info("Pagamento negado -> delegando para FSM");
                    policyStateMachine.onPaymentDenied(policy);
                }
                case "CONFIRMED" -> {
                    log.info("Pagamento confirmado -> marcando em memória e delegando para FSM");
                    inMemoryCorrelationStore.markPayment(e.requestId(), e.occurredAt());
                    policyStateMachine.onPaymentConfirmed(policy);
                }
                default -> log.warn("Status de pagamento não reconhecido: {} (ignorando)", e.status());
            }
        }, () -> log.warn("Policy não encontrada para requestId={} (ignorando payment)", e.requestId()));
    }
}