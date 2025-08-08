package com.acme.insurance.policy.domain.service;

import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.app.dto.fraud.FraudAnalysisResponse;
import com.acme.insurance.policy.domain.ports.in.PolicyService;
import com.acme.insurance.policy.domain.ports.out.FraudGateway;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class PolicyServiceImpl implements PolicyService {

    private final FraudGateway fraudGateway;

    public PolicyServiceImpl(FraudGateway fraudGateway) {
        this.fraudGateway = fraudGateway;
    }

    @Override
    public PolicyResponseDto createPolicy(PolicyRequestDto request) {
        FraudAnalysisResponse fraud = fraudGateway.analyze(UUID.randomUUID(), request.customerId());
        return new PolicyResponseDto(
                UUID.randomUUID(),
                request.customerId(),
                request.productId(),
                request.category(),
                request.salesChannel(),
                request.paymentMethod(),
                "RECEIVED",
                OffsetDateTime.now(),
                null,
                request.totalMonthlyPremiumAmount(),
                request.insuredAmount(),
                request.coverages(),
                request.assistances(),
                Collections.emptyList()
        );
    }

    @Override
    public PolicyResponseDto getPolicyById(UUID id) {
        return null;
    }

    @Override
    public List<PolicyResponseDto> getPoliciesByCustomerId(UUID customerId) {
        return Collections.emptyList();
    }
}
