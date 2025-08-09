package com.acme.insurance.policy.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

    public Policy withStatusAndHistory(String newStatus, OffsetDateTime at) {
        var newHistory = new ArrayList<>(history == null ? List.of() : history);
        newHistory.add(new StatusHistory(newStatus, at));
        return new Policy(
                id, customerId, productId, category, salesChannel, paymentMethod,
                newStatus, createdAt, finishedAt, coverages, assistances,
                totalMonthlyPremiumAmount, insuredAmount, List.copyOf(newHistory)
        );
    }

    public Policy withFinishedAt(OffsetDateTime at) {
        return new Policy(
                id, customerId, productId, category, salesChannel, paymentMethod,
                status, createdAt, at, coverages, assistances,
                totalMonthlyPremiumAmount, insuredAmount, history
        );
    }

    public Policy withStatusNow(String newStatus) {
        return withStatusAndHistory(newStatus, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
