package com.acme.insurance.policy.domain.ports.in;

import com.acme.insurance.policy.app.dto.PolicyResponseDto;

import java.util.List;
import java.util.UUID;

public interface ListPoliciesByCustomerQuery {
    List<PolicyResponseDto> execute(UUID customerId);
}
