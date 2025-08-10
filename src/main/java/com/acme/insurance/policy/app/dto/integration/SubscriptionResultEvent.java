package com.acme.insurance.policy.app.dto.integration;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResultEvent(
        UUID requestId,
        String status,
        Instant occurredAt
) {}
