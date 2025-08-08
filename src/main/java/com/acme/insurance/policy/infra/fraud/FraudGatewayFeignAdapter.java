package com.acme.insurance.policy.infra.fraud;

import com.acme.insurance.policy.app.dto.fraud.FraudAnalysisResponse;
import com.acme.insurance.policy.domain.ports.out.FraudGateway;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FraudGatewayFeignAdapter implements FraudGateway {

    private final FraudClient client;

    public FraudGatewayFeignAdapter(FraudClient client) {
        this.client = client;
    }

    @Override
    public FraudAnalysisResponse analyze(UUID orderId, UUID customerId) {
        return client.analyze(orderId.toString(), customerId.toString());
    }
}