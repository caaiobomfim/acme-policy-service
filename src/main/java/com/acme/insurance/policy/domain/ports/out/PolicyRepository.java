package com.acme.insurance.policy.domain.ports.out;

import com.acme.insurance.policy.domain.model.Policy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository {

    void save(Policy policy);

    Optional<Policy> findById(UUID policyId);

    List<Policy> findByCustomerId(UUID customerId);
}
