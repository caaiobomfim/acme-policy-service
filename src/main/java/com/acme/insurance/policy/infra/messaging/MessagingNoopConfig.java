package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import com.acme.insurance.policy.domain.events.PolicyRequestStatusChangedEvent;
import com.acme.insurance.policy.domain.ports.out.PolicyRequestPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(PolicyRequestPublisher.class)
public class MessagingNoopConfig {

    @Bean
    public PolicyRequestPublisher noopPolicyRequestPublisher() {
        return new PolicyRequestPublisher() {

            @Override
            public void publish(PolicyRequestCreatedEvent e) {

            }
            @Override
            public void publish(PolicyRequestStatusChangedEvent e) {

            }
        };
    }
}
