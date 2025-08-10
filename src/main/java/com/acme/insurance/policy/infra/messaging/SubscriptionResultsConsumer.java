package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.app.dto.integration.SubscriptionResultEvent;
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
public class SubscriptionResultsConsumer {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionResultsConsumer.class);

    private static final String MARK_PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";
    private static final String MARK_SUBSCRIPTION_AUTHORIZED = "SUBSCRIPTION_AUTHORIZED";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final String REASON_BY_SUBS = "BY_SUBSCRIPTION";
    private static final String REASON_PAYMENT_PLUS_SUB = "PAYMENT+SUBSCRIPTION";

    private final PolicyDynamoRepository policyDynamoRepository;
    private final PolicyItemMapper policyItemMapper;
    private final PolicyRequestPublisher policyRequestPublisher;

    public SubscriptionResultsConsumer(PolicyDynamoRepository policyDynamoRepository,
                                       PolicyItemMapper policyItemMapper,
                                       PolicyRequestPublisher policyRequestPublisher) {
        this.policyDynamoRepository = policyDynamoRepository;
        this.policyItemMapper = policyItemMapper;
        this.policyRequestPublisher = policyRequestPublisher;
    }

    @SqsListener("http://localhost:4566/000000000000/insurance-subscriptions-topic")
    public void onSubscription(SubscriptionResultEvent e){
        log.info("[SQS] Mensagem recebida - requestId={} status={}", e.requestId(), e.status());
        policyDynamoRepository.findById(e.requestId().toString())
                .map(policyItemMapper::toDomain)
                .ifPresent(policyFound -> {

            log.info("Policy encontrada - id={} statusAtual={}", policyFound.id(), policyFound.status());
            OffsetDateTime at = OffsetDateTime.ofInstant(e.occurredAt(), ZoneOffset.UTC);

            if ("DENIED".equalsIgnoreCase(e.status())) {
                log.info("Subscrição negada. Alterando status para REJECTED");
                Policy rejected = policyFound
                        .withStatusAndHistory(STATUS_REJECTED, at)
                        .withFinishedAt(at);
                policyDynamoRepository.save(policyItemMapper.toItem(rejected));

                policyRequestPublisher.publish(new PolicyRequestStatusChangedEvent(
                        rejected.id(),
                        rejected.customerId(),
                        rejected.productId(),
                        STATUS_REJECTED,
                        REASON_BY_SUBS,
                        e.occurredAt()
                ));
                return;
            }

            if ("AUTHORIZED".equalsIgnoreCase(e.status())) {
                log.info("Subscrição autorizada. Registrando marcador {} no histórico (status principal permanece).",
                        MARK_SUBSCRIPTION_AUTHORIZED);

                Policy withSubsHistory = policyFound.withHistoryEntry(MARK_SUBSCRIPTION_AUTHORIZED, at);
                policyDynamoRepository.save(policyItemMapper.toItem(withSubsHistory));

                boolean hasPayment = withSubsHistory.hasHistory(MARK_PAYMENT_CONFIRMED);
                boolean hasSubs = withSubsHistory.hasHistory(MARK_SUBSCRIPTION_AUTHORIZED);

                log.info("Pagamento confirmado no histórico? {} | Subscrição autorizada no histórico? {}", hasPayment, hasSubs);

                if (STATUS_PENDING.equalsIgnoreCase(withSubsHistory.status()) && hasPayment && hasSubs) {
                    log.info("Pagamento + Subscrição confirmados. Alterando status para APPROVED");
                    OffsetDateTime okAt = at;
                    Policy approved = withSubsHistory
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
                log.warn("Status de subscrição não reconhecido: {} (ignorando)", e.status());
            }
        });
    }
}