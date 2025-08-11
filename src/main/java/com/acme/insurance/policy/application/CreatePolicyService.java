package com.acme.insurance.policy.application;

import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.app.dto.fraud.FraudAnalysisResponse;
import com.acme.insurance.policy.app.mapper.ApiPolicyMapper;
import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import com.acme.insurance.policy.domain.fraud.FraudClassification;
import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import com.acme.insurance.policy.domain.ports.in.CreatePolicyUseCase;
import com.acme.insurance.policy.domain.ports.out.FraudGateway;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import com.acme.insurance.policy.domain.ports.out.PolicyRequestPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CreatePolicyService implements CreatePolicyUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreatePolicyService.class);

    private final PolicyRepository policyRepository;
    private final PolicyRequestPublisher policyRequestPublisher;
    private final FraudGateway fraudGateway;
    private final PolicyStateMachine policyStateMachine;
    private final ApiPolicyMapper apiPolicyMapper;

    public CreatePolicyService(PolicyRepository policyRepository,
                               PolicyRequestPublisher policyRequestPublisher,
                               FraudGateway fraudGateway,
                               PolicyStateMachine policyStateMachine,
                               ApiPolicyMapper apiPolicyMapper) {
        this.policyRepository = policyRepository;
        this.policyRequestPublisher = policyRequestPublisher;
        this.fraudGateway = fraudGateway;
        this.policyStateMachine = policyStateMachine;
        this.apiPolicyMapper = apiPolicyMapper;
    }

    @Override
    public PolicyResponseDto execute(PolicyRequestDto request) {
        log.info("[USECASE] Criando policy para customerId={} productId={}", request.customerId(), request.productId());

        Policy base = apiPolicyMapper.toDomain(request);

        Policy policy = new Policy(
                UUID.randomUUID(),
                base.customerId(),
                base.productId(),
                base.category(),
                base.salesChannel(),
                base.paymentMethod(),
                PolicyStatus.RECEIVED,
                base.createdAt(),
                null,
                base.coverages(),
                base.assistances(),
                base.totalMonthlyPremiumAmount(),
                base.insuredAmount(),
                List.of(new Policy.StatusHistory(PolicyStatus.RECEIVED, base.createdAt()))
        );

        policyRepository.save(policy);
        policyRequestPublisher.publish(new PolicyRequestCreatedEvent(
                policy.id(),
                policy.customerId(),
                policy.productId(),
                PolicyStatus.RECEIVED.name(),
                Instant.now()
        ));

        FraudAnalysisResponse fraud = fraudGateway.analyze(policy.id(), policy.customerId());
        FraudClassification classification = FraudClassification.from(fraud.classification());
        log.info("[USECASE] Resultado fraude id={} classification={}", policy.id(), classification);
        policyStateMachine.onFraud(policy, classification, policy.category(), policy.insuredAmount());

        Policy persisted = policyRepository.findById(policy.id()).orElse(policy);
        log.info("[USECASE] Policy processada - id={} status={}", persisted.id(), persisted.status());
        return apiPolicyMapper.toResponse(persisted);
    }
}
