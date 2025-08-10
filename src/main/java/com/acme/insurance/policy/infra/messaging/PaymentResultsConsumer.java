package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.app.dto.integration.PaymentResultEvent;
import com.acme.insurance.policy.domain.events.PolicyRequestStatusChangedEvent;
import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.infra.dynamodb.PolicyDynamoRepository;
import com.acme.insurance.policy.infra.dynamodb.mapper.PolicyItemMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class PaymentResultsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultsConsumer.class);

    private static final String MARK_PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";
    private static final String MARK_SUBSCRIPTION_AUTHORIZED = "SUBSCRIPTION_AUTHORIZED";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final String REASON_BY_PAYMENT = "BY_PAYMENT";
    private static final String REASON_PAYMENT_PLUS_SUB = "PAYMENT+SUBSCRIPTION";

    private final PolicyDynamoRepository policyDynamoRepository;
    private final PolicyItemMapper policyItemMapper;
    private final PolicyRequestPublisher policyRequestPublisher;

    public PaymentResultsConsumer(PolicyDynamoRepository policyDynamoRepository,
                                  PolicyItemMapper policyItemMapper,
                                  PolicyRequestPublisher policyRequestPublisher) {
        this.policyDynamoRepository = policyDynamoRepository;
        this.policyItemMapper = policyItemMapper;
        this.policyRequestPublisher = policyRequestPublisher;
    }

    @SqsListener("http://localhost:4566/000000000000/payments-topic")
    public void onPayment(PaymentResultEvent e){
        log.info("[SQS] Mensagem recebida - requestId={} status={}", e.requestId(), e.status());
        policyDynamoRepository.findById(e.requestId().toString())
                .map(policyItemMapper::toDomain)
                .ifPresent(policyFound -> {

            log.info("Policy encontrada - id={} statusAtual={}", policyFound.id(), policyFound.status());
            OffsetDateTime at = OffsetDateTime.ofInstant(e.occurredAt(), ZoneOffset.UTC);

            if ("DENIED".equalsIgnoreCase(e.status())) {
                log.info("Pagamento negado. Alterando status para REJECTED");
                Policy rejected = policyFound
                        .withStatusAndHistory(STATUS_REJECTED, at)
                        .withFinishedAt(at);
                policyDynamoRepository.save(policyItemMapper.toItem(rejected));

                policyRequestPublisher.publish(new PolicyRequestStatusChangedEvent(
                        rejected.id(),
                        rejected.customerId(),
                        rejected.productId(),
                        STATUS_REJECTED,
                        REASON_BY_PAYMENT,
                        e.occurredAt()
                ));
                return;
            }

            if ("CONFIRMED".equalsIgnoreCase(e.status())) {
                log.info("Pagamento confirmado. Registrando marcador {} no histórico (status principal permanece).",
                        MARK_PAYMENT_CONFIRMED);

                Policy withPaymentHistory = policyFound.withHistoryEntry(MARK_PAYMENT_CONFIRMED, at);
                policyDynamoRepository.save(policyItemMapper.toItem(withPaymentHistory));

                boolean hasPayment = withPaymentHistory.hasHistory(MARK_PAYMENT_CONFIRMED);
                boolean hasSubs = withPaymentHistory.hasHistory(MARK_SUBSCRIPTION_AUTHORIZED);

                log.info("Pagamento no histórico? {} | Subscrição autorizada no histórico? {}", hasPayment, hasSubs);

                if (STATUS_PENDING.equalsIgnoreCase(withPaymentHistory.status()) && hasPayment && hasSubs) {
                    log.info("Pagamento + Subscrição confirmados. Alterando status para APPROVED");
                    OffsetDateTime okAt = at;
                    Policy approved = withPaymentHistory
                            .withStatusAndHistory(STATUS_APPROVED, okAt)
                            .withFinishedAt(okAt);

                    policyDynamoRepository.save(policyItemMapper.toItem(approved));

                    policyRequestPublisher.publish(new PolicyRequestStatusChangedEvent(
                            approved.id(),
                            approved.customerId(),
                            approved.productId(),
                            STATUS_APPROVED,
                            REASON_PAYMENT_PLUS_SUB,
                            okAt.toInstant()
                    ));
                }
            } else {
                log.warn("Status de pagamento não reconhecido: {} (ignorando)", e.status());
            }
        });
    }
}