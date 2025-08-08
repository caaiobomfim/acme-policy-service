package com.acme.insurance.policy.app.dto.fraud;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record FraudAnalysisResponse(
        @JsonProperty("orderId") String orderId,
        @JsonProperty("customerId") String customerId,
        @JsonProperty("analyzedAt") OffsetDateTime analyzedAt,
        @JsonProperty("classification") String classification,
        @JsonProperty("occurrences") List<Occurrence> occurrences
) {
    public record Occurrence(
            @JsonProperty("id") String id,
            @JsonProperty("productId") Long productId,
            @JsonProperty("type") String type,
            @JsonProperty("description") String description,
            @JsonProperty("createdAt") OffsetDateTime createdAt,
            @JsonProperty("updatedAt") OffsetDateTime updatedAt
    ) {}
}
