package com.acme.insurance.policy.domain.ports.in;

import com.acme.insurance.policy.app.dto.PolicyResponseDto;

import java.util.UUID;

public interface GetPolicyByIdQuery {
    PolicyResponseDto execute(UUID id);
}
