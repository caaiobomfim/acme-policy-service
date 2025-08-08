package com.acme.insurance.policy.domain.ports.in;

import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;

import java.util.List;
import java.util.UUID;

public interface PolicyService {

    PolicyResponseDto createPolicy(PolicyRequestDto request);

    PolicyResponseDto getPolicyById(UUID id);

    List<PolicyResponseDto> getPoliciesByCustomerId(UUID customerId);
}
