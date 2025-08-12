package com.acme.insurance.policy.application;

import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.app.error.PolicyNotFoundByIdCustomerException;
import com.acme.insurance.policy.app.mapper.ApiPolicyMapper;
import com.acme.insurance.policy.domain.ports.in.ListPoliciesByCustomerQuery;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ListPoliciesByCustomerService implements ListPoliciesByCustomerQuery {

    private static final Logger log = LoggerFactory.getLogger(ListPoliciesByCustomerService.class);

    private final PolicyRepository policyRepository;
    private final ApiPolicyMapper apiPolicyMapper;

    public ListPoliciesByCustomerService(PolicyRepository policyRepository,
                                         ApiPolicyMapper apiPolicyMapper) {
        this.policyRepository = policyRepository;
        this.apiPolicyMapper = apiPolicyMapper;
    }

    @Override
    public List<PolicyResponseDto> execute(UUID customerId) {
        log.info("[USECASE] Listando policies por customerId={}", customerId);
        var list = policyRepository.findByCustomerId(customerId)
                .stream()
                .map(apiPolicyMapper::toResponse)
                .toList();

        if (list.isEmpty()) {
            log.warn("[USECASE] Nenhuma policy encontrada para customerId={}", customerId);
            throw new PolicyNotFoundByIdCustomerException(customerId);
        }

        log.info("[USECASE] Encontradas {} policies para customerId={}", list.size(), customerId);
        return list;
    }
}