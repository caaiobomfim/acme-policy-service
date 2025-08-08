package com.acme.insurance.policy.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PolicyResponseDto(
        @JsonProperty("id")
        UUID id,
        @JsonProperty("customer_id")
        UUID customerId,
        @JsonProperty("product_id")
        UUID productId,
        @JsonProperty("category")
        String category,
        @JsonProperty("salesChannel")
        String salesChannel,
        @JsonProperty("paymentMethod")
        String paymentMethod,
        @JsonProperty("status")
        String status,
        @JsonProperty("createdAt")
        OffsetDateTime createdAt,
        @JsonProperty("finishedAt")
        OffsetDateTime finishedAt,
        @JsonProperty("total_monthly_premium_amount")
        BigDecimal totalMonthlyPremiumAmount,
        @JsonProperty("insured_amount")
        BigDecimal insuredAmount,
        @JsonProperty("coverages")
        Map<String, BigDecimal> coverages,
        @JsonProperty("assistances")
        List<String> assistances,
        @JsonProperty("history")
        List<StatusHistoryDto> history
) {
    public record StatusHistoryDto(
            @JsonProperty("status")
            String status,
            @JsonProperty("timestamp")
            OffsetDateTime timestamp
    ){}
}
