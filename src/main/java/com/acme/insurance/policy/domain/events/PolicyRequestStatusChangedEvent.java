package com.acme.insurance.policy.domain.events;

import java.time.Instant;

public record PolicyRequestStatusChangedEvent(
        java.util.UUID requestId,
        java.util.UUID customerId,
        java.util.UUID productId,
        String status,
        String riskClassification,
        Instant occurredAt
) {}
