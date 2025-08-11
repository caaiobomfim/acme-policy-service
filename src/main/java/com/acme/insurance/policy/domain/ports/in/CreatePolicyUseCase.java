package com.acme.insurance.policy.domain.ports.in;

import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;

public interface CreatePolicyUseCase {
    PolicyResponseDto execute(PolicyRequestDto request);
}
