package com.acme.insurance.policy.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PolicyRequestDto(

        @NotNull
        @JsonProperty("customer_id")
        UUID customerId,

        @NotNull
        @JsonProperty("product_id")
        UUID productId,

        @NotBlank
        @JsonProperty("category")
        String category,

        @NotBlank
        @JsonProperty("salesChannel")
        String salesChannel,

        @NotBlank
        @JsonProperty("paymentMethod")
        String paymentMethod,

        @NotNull
        @DecimalMin(value = "0.01")
        @JsonProperty("total_monthly_premium_amount")
        BigDecimal totalMonthlyPremiumAmount,

        @NotNull
        @DecimalMin(value = "0.01")
        @JsonProperty("insured_amount")
        BigDecimal insuredAmount,

        @NotEmpty
        @JsonProperty("coverages")
        Map<String, BigDecimal> coverages,

        @NotEmpty
        @JsonProperty("assistances")
        List<String> assistances
) {}