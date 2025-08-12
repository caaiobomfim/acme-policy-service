package com.acme.insurance.policy.domain.fraud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static com.acme.insurance.policy.domain.fraud.FraudClassification.*;
import static com.acme.insurance.policy.domain.fraud.FraudRules.isApproved;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FraudRulesTest {

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    static Stream<Arguments> boundaryCases() {
        return Stream.of(

                // REGULAR
                arguments(REGULAR, "AUTO",        "350000", true),
                arguments(REGULAR, "AUTO",        "350001", false),
                arguments(REGULAR, "LIFE",        "500000", true),
                arguments(REGULAR, "LIFE",        "500001", false),
                arguments(REGULAR, "RESIDENTIAL", "500000", true),
                arguments(REGULAR, "RESIDENTIAL", "500001", false),
                arguments(REGULAR, "PET",         "255000", true),
                arguments(REGULAR, "PET",         "255001", false),
                arguments(REGULAR, "auto",        "350000", true),

                // HIGH_RISK
                arguments(HIGH_RISK, "AUTO",        "250000", true),
                arguments(HIGH_RISK, "AUTO",        "250001", false),
                arguments(HIGH_RISK, "RESIDENTIAL", "150000", true),
                arguments(HIGH_RISK, "RESIDENTIAL", "150001", false),
                arguments(HIGH_RISK, "LIFE",        "125000", true),
                arguments(HIGH_RISK, "LIFE",        "125001", false),

                // PREFERENTIAL
                arguments(PREFERENTIAL, "LIFE",        "800000", true),
                arguments(PREFERENTIAL, "LIFE",        "800001", false),
                arguments(PREFERENTIAL, "AUTO",        "450000", true),
                arguments(PREFERENTIAL, "AUTO",        "450001", false),
                arguments(PREFERENTIAL, "RESIDENTIAL", "450000", true),
                arguments(PREFERENTIAL, "RESIDENTIAL", "450001", false),
                arguments(PREFERENTIAL, "OTHER",       "375000", true),
                arguments(PREFERENTIAL, "OTHER",       "375001", false),

                // NO_INFO
                arguments(NO_INFO, "AUTO",        "75000",  true),
                arguments(NO_INFO, "AUTO",        "75001",  false),
                arguments(NO_INFO, "LIFE",        "200000", true),
                arguments(NO_INFO, "LIFE",        "200001", false),
                arguments(NO_INFO, "RESIDENTIAL", "200000", true),
                arguments(NO_INFO, "RESIDENTIAL", "200001", false),
                arguments(NO_INFO, "OTHER",       "55000",  true),
                arguments(NO_INFO, "OTHER",       "55001",  false)
        );
    }

    @ParameterizedTest(name = "{index} => {0} {1} {2} -> {3}")
    @MethodSource("boundaryCases")
    @DisplayName("isApproved: limites (<=) por classificação e categoria, incluindo default/case-insensitive")
    void isApproved_boundaries(FraudClassification cls, String category, String amount, boolean expected) {
        boolean ok = isApproved(cls, category, bd(amount));
        assertThat(ok).isEqualTo(expected);
    }

    @Test
    @DisplayName("Categoria null cai no branch default (REGULAR -> 255000)")
    void nullCategory_usesDefaultBranch() {
        assertThat(isApproved(REGULAR, null, bd("255000"))).isTrue();
        assertThat(isApproved(REGULAR, null, bd("255001"))).isFalse();
    }
}