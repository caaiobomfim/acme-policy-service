package com.acme.insurance.policy.domain.ports.out;

import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import com.acme.insurance.policy.domain.events.PolicyRequestStatusChangedEvent;

public interface PolicyRequestPublisher {

    void publish(PolicyRequestCreatedEvent event);
    void publish(PolicyRequestStatusChangedEvent event);
}
