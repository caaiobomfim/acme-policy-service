package com.acme.insurance.policy.domain.fraud;

import java.math.BigDecimal;

public class FraudRules {

    public static boolean isApproved(FraudClassification cls, String category, BigDecimal insuredAmount) {

        category = category == null ? "" : category.toUpperCase();

        return switch (cls) {
            case REGULAR -> switch (category) {
                case "LIFE", "RESIDENTIAL" -> insuredAmount.compareTo(new BigDecimal("500000")) <= 0;
                case "AUTO"                -> insuredAmount.compareTo(new BigDecimal("350000")) <= 0;
                default                    -> insuredAmount.compareTo(new BigDecimal("255000")) <= 0;
            };
            case HIGH_RISK -> switch (category) {
                case "AUTO"        -> insuredAmount.compareTo(new BigDecimal("250000")) <= 0;
                case "RESIDENTIAL" -> insuredAmount.compareTo(new BigDecimal("150000")) <= 0;
                default            -> insuredAmount.compareTo(new BigDecimal("125000")) <= 0;
            };
            case PREFERENTIAL -> switch (category) {
                case "LIFE"                     -> insuredAmount.compareTo(new BigDecimal("800000")) <= 0;
                case "AUTO", "RESIDENTIAL"      -> insuredAmount.compareTo(new BigDecimal("450000")) <= 0;
                default                         -> insuredAmount.compareTo(new BigDecimal("375000")) <= 0;
            };
            case NO_INFO -> switch (category) {
                case "LIFE", "RESIDENTIAL" -> insuredAmount.compareTo(new BigDecimal("200000")) <= 0;
                case "AUTO"                -> insuredAmount.compareTo(new BigDecimal("75000"))  <= 0;
                default                    -> insuredAmount.compareTo(new BigDecimal("55000"))  <= 0;
            };
        };
    }
}
