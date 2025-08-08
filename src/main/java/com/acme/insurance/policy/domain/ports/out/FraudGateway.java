package com.acme.insurance.policy.domain.ports.out;

import com.acme.insurance.policy.app.dto.fraud.FraudAnalysisResponse;

import java.util.UUID;

public interface FraudGateway {
    FraudAnalysisResponse analyze(UUID orderId, UUID customerId);
}
