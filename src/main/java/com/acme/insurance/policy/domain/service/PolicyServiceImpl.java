package com.acme.insurance.policy.domain.service;

import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.domain.ports.PolicyService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class PolicyServiceImpl implements PolicyService {

    @Override
    public PolicyResponseDto createPolicy(PolicyRequestDto request) {
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
