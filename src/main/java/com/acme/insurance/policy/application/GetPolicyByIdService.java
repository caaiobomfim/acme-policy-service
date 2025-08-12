package com.acme.insurance.policy.application;

import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.app.error.PolicyNotFoundException;
import com.acme.insurance.policy.app.mapper.ApiPolicyMapper;
import com.acme.insurance.policy.domain.ports.in.GetPolicyByIdQuery;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetPolicyByIdService implements GetPolicyByIdQuery {

    private static final Logger log = LoggerFactory.getLogger(GetPolicyByIdService.class);

    private final PolicyRepository policyRepository;
    private final ApiPolicyMapper apiPolicyMapper;

    public GetPolicyByIdService(PolicyRepository policyRepository,
                                ApiPolicyMapper apiPolicyMapper) {
        this.policyRepository = policyRepository;
        this.apiPolicyMapper = apiPolicyMapper;
    }

    @Override
    public PolicyResponseDto execute(UUID id) {
        log.info("[USECASE] Buscando policy por id={}", id);
        return policyRepository.findById(id)
                .map(apiPolicyMapper::toResponse)
                .orElseThrow(() -> {
                    log.warn("[USECASE] Policy não encontrada - id={}", id);
                    return new PolicyNotFoundException(id);
                });
    }
}
