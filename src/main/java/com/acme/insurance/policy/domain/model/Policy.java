package com.acme.insurance.policy.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Policy(
        UUID id,
        UUID customerId,
        UUID productId,
        String category,
        String salesChannel,
        String paymentMethod,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime finishedAt,
        Map<String, BigDecimal> coverages,
        List<String> assistances,
        BigDecimal totalMonthlyPremiumAmount,
        BigDecimal insuredAmount,
        List<StatusHistory> history
) {
    public record StatusHistory(
            String status,
            OffsetDateTime timestamp
    ) {}
}
