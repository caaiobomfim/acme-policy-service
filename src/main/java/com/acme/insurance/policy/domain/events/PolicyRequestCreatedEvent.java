package com.acme.insurance.policy.domain.events;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;
import java.util.UUID;

public record PolicyRequestCreatedEvent(
        UUID requestId,
        UUID customerId,
        UUID productId,
        String status,
        @JsonSerialize(using = ToStringSerializer.class)
        Instant occurredAt
) {}
