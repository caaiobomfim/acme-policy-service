package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import com.acme.insurance.policy.domain.events.PolicyRequestStatusChangedEvent;
import com.acme.insurance.policy.domain.ports.out.PolicyRequestPublisher;
import com.acme.insurance.policy.infra.config.AppProps;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.sqs.enabled", havingValue = "true", matchIfMissing = true)
public class SqsPolicyRequestPublisher implements PolicyRequestPublisher {

    private static final Logger log = LoggerFactory.getLogger(SqsPolicyRequestPublisher.class);

    private final String queueName;
    private final SqsTemplate sqsTemplate;

    public SqsPolicyRequestPublisher(SqsTemplate sqsTemplate, AppProps props) {
        this.sqsTemplate = sqsTemplate;
        this.queueName = props.sqs().queues().orders();
    }

    @Override
    public void publish(PolicyRequestCreatedEvent event) {
        log.info("[SQS] Enviando PolicyRequestCreatedEvent para a fila {} - policyId={} status={}",
                queueName, event.requestId(), event.status());
        sqsTemplate.send(to -> to
                .queue(queueName)
                .header("eventType", "PolicyRequestCreatedEvent")
                .header("status", event.status())
                .payload(event));
        log.info("[SQS] PolicyRequestCreatedEvent enviado com sucesso - policyId={}", event.requestId());
    }

    @Override
    public void publish(PolicyRequestStatusChangedEvent event) {
        log.info("[SQS] Enviando PolicyRequestStatusChangedEvent para a fila {} - policyId={} status={}",
                queueName, event.requestId(), event.status());
        sqsTemplate.send(to -> to
                .queue(queueName)
                .header("eventType", "PolicyRequestStatusChangedEvent")
                .header("status", event.status())
                .payload(event));
        log.info("[SQS] PolicyRequestStatusChangedEvent enviado com sucesso - policyId={}", event.requestId());
    }
}
