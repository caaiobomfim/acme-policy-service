package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PolicyRequestConsumer {

    private static final Logger log = LoggerFactory.getLogger(PolicyRequestConsumer.class);

    @SqsListener("orders-topic")
    public void onMessage(PolicyRequestCreatedEvent event, @Headers Map<String, Object> headers) {
        log.info("SQS received: {}, headers={}", event, headers);
    }
}
