package com.acme.insurance.policy.application;

import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.app.mapper.ApiPolicyMapper;
import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.ports.in.CancelPolicyUseCase;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class CancelPolicyService implements CancelPolicyUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelPolicyService.class);

    private final PolicyRepository policyRepository;
    private final PolicyStateMachine policyStateMachine;
    private final ApiPolicyMapper apiPolicyMapper;

    public CancelPolicyService(PolicyRepository policyRepository,
                               PolicyStateMachine policyStateMachine,
                               ApiPolicyMapper apiPolicyMapper) {
        this.policyRepository = policyRepository;
        this.policyStateMachine = policyStateMachine;
        this.apiPolicyMapper = apiPolicyMapper;
    }

    @Override
    public PolicyResponseDto execute(UUID id) {
        log.info("[USECASE] Cancelamento solicitado - id={}", id);

        Policy policy = policyRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy não encontrada"));

        if (policy.isFinalStatus()) {
            log.warn("[USECASE] Cancelamento inválido - id={} statusAtual={}", policy.id(), policy.status());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível cancelar uma policy com status final: " + policy.status()
            );
        }

        Policy updated = policyStateMachine.cancel(policy, "BY_CUSTOMER_REQUEST");

        Policy persisted = policyRepository.findById(updated.id()).orElse(updated);
        log.info("[USECASE] Policy cancelada - id={} status={}", persisted.id(), persisted.status());
        return apiPolicyMapper.toResponse(persisted);
    }
}
