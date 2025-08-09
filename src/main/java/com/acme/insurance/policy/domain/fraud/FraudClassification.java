package com.acme.insurance.policy.domain.fraud;

public enum FraudClassification {
    REGULAR, HIGH_RISK, PREFERENTIAL, NO_INFO;

    public static FraudClassification from(String s) {
        return switch (s) {
            case "REGULAR"      -> REGULAR;
            case "HIGH_RISK"    -> HIGH_RISK;
            case "PREFERENTIAL" -> PREFERENTIAL;
            default             -> NO_INFO;
        };
    }
}
