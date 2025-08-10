package com.acme.insurance.policy.infra.fraud;

import com.acme.insurance.policy.app.dto.fraud.FraudAnalysisResponse;
import com.acme.insurance.policy.domain.ports.out.FraudGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FraudGatewayFeignAdapter implements FraudGateway {

    private static final Logger log = LoggerFactory.getLogger(FraudGatewayFeignAdapter.class);

    private final FraudClient client;

    public FraudGatewayFeignAdapter(FraudClient client) {
        this.client = client;
    }

    @Override
    public FraudAnalysisResponse analyze(UUID orderId, UUID customerId) {
        log.info("[FRAUD] Iniciando análise de fraude - orderId={} customerId={}", orderId, customerId);
        FraudAnalysisResponse response = client.analyze(orderId.toString(), customerId.toString());
        log.info("[FRAUD] Resultado recebido - classification={}", response.classification());
        return response;
    }
}