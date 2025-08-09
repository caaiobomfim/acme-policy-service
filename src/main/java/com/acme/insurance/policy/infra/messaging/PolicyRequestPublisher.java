package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import com.acme.insurance.policy.domain.events.PolicyRequestStatusChangedEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.stereotype.Component;

@Component
public class PolicyRequestPublisher {

    private static final String QUEUE_NAME = "orders-topic";
    private final SqsTemplate sqsTemplate;

    public PolicyRequestPublisher(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    public void publish(PolicyRequestCreatedEvent event) {
        sqsTemplate.send(to -> to.queue(QUEUE_NAME).payload(event));
    }

    public void publish(PolicyRequestStatusChangedEvent event) {
        sqsTemplate.send(to -> to.queue(QUEUE_NAME).payload(event));
    }
}
