package com.acme.insurance.policy.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

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
        @Positive(message = "amount must be positive")
        @Digits(integer = 12, fraction = 2, message = "amount must have up to 2 decimal places")
        BigDecimal totalMonthlyPremiumAmount,

        @NotNull
        @DecimalMin(value = "0.01")
        @JsonProperty("insured_amount")
        @Positive(message = "amount must be positive")
        @Digits(integer = 12, fraction = 2, message = "amount must have up to 2 decimal places")
        BigDecimal insuredAmount,

        @NotEmpty
        @JsonProperty("coverages")
        Map<String,
                @Positive(message = "coverage must be positive")
                @Digits(integer = 12, fraction = 2, message = "coverage must have up to 2 decimal places")
                BigDecimal> coverages,

        @NotEmpty
        @JsonProperty("assistances")
        List<String> assistances
) {}