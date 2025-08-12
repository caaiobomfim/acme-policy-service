package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import com.acme.insurance.policy.domain.events.PolicyRequestStatusChangedEvent;
import com.acme.insurance.policy.domain.ports.out.PolicyRequestPublisher;
import com.acme.insurance.policy.infra.config.AppProps;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsPolicyRequestPublisherTest {

    @Mock
    SqsTemplate sqsTemplate;

    private AppProps propsWithQueue(String queue) {
        AppProps props = mock(AppProps.class, RETURNS_DEEP_STUBS);
        when(props.sqs().queues().orders()).thenReturn(queue);
        return props;
    }

    @Test
    @DisplayName("publish(PolicyRequestCreatedEvent) delega para SqsTemplate.send(Consumer)")
    void publish_created_delegates() {
        AppProps props = propsWithQueue("orders-queue-test");
        PolicyRequestPublisher publisher = new SqsPolicyRequestPublisher(sqsTemplate, props);

        var event = mock(PolicyRequestCreatedEvent.class);
        when(event.requestId()).thenReturn(UUID.randomUUID());
        when(event.status()).thenReturn("RECEIVED");

        publisher.publish(event);

        verify(sqsTemplate, times(1)).send(any(Consumer.class));
        verifyNoMoreInteractions(sqsTemplate);
    }

    @Test
    @DisplayName("publish(PolicyRequestStatusChangedEvent) delega para SqsTemplate.send(Consumer)")
    void publish_statusChanged_delegates() {
        AppProps props = propsWithQueue("orders-queue-test");
        PolicyRequestPublisher publisher = new SqsPolicyRequestPublisher(sqsTemplate, props);

        var event = mock(PolicyRequestStatusChangedEvent.class);
        when(event.requestId()).thenReturn(UUID.randomUUID());
        when(event.status()).thenReturn("APPROVED");

        publisher.publish(event);

        verify(sqsTemplate, times(1)).send(any(Consumer.class));
        verifyNoMoreInteractions(sqsTemplate);
    }
}