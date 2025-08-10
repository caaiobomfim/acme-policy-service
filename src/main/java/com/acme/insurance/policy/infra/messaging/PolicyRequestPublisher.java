package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import com.acme.insurance.policy.domain.events.PolicyRequestStatusChangedEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PolicyRequestPublisher {

    private static final Logger log = LoggerFactory.getLogger(PolicyRequestPublisher.class);

    private static final String QUEUE_NAME = "orders-topic";
    private final SqsTemplate sqsTemplate;

    public PolicyRequestPublisher(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    public void publish(PolicyRequestCreatedEvent event) {
        log.info("[SQS] Enviando PolicyRequestCreatedEvent para a fila {} - policyId={} status={}",
                QUEUE_NAME, event.requestId(), event.status());
        sqsTemplate.send(to -> to
                .queue(QUEUE_NAME)
                .header("eventType", "PolicyRequestCreatedEvent")
                .header("status", event.status())
                .payload(event));
        log.info("[SQS] PolicyRequestCreatedEvent enviado com sucesso - policyId={}", event.requestId());
    }

    public void publish(PolicyRequestStatusChangedEvent event) {
        log.info("[SQS] Enviando PolicyRequestStatusChangedEvent para a fila {} - policyId={} status={}",
                QUEUE_NAME, event.requestId(), event.status());
        sqsTemplate.send(to -> to
                .queue(QUEUE_NAME)
                .header("eventType", "PolicyRequestStatusChangedEvent")
                .header("status", event.status())
                .payload(event));
        log.info("[SQS] PolicyRequestStatusChangedEvent enviado com sucesso - policyId={}", event.requestId());
    }
}
