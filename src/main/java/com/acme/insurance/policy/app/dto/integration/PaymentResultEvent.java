package com.acme.insurance.policy.app.dto.integration;

import java.time.Instant;
import java.util.UUID;

public record PaymentResultEvent(
        UUID requestId,
        String paymentId,
        String status,
        Instant occurredAt
) {}