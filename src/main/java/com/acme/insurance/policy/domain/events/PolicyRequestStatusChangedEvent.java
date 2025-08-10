package com.acme.insurance.policy.domain.events;

import java.time.Instant;
import java.util.UUID;

public record PolicyRequestStatusChangedEvent(
        UUID requestId,
        UUID customerId,
        UUID productId,
        String status,
        String riskClassification,
        Instant occurredAt
) {}
