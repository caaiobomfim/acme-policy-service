package com.acme.insurance.policy.domain.service;

import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.app.dto.fraud.FraudAnalysisResponse;
import com.acme.insurance.policy.app.mapper.ApiPolicyMapper;
import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import com.acme.insurance.policy.domain.events.PolicyRequestStatusChangedEvent;
import com.acme.insurance.policy.domain.fraud.FraudClassification;
import com.acme.insurance.policy.domain.fraud.FraudRules;
import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.ports.in.PolicyService;
import com.acme.insurance.policy.domain.ports.out.FraudGateway;
import com.acme.insurance.policy.infra.dynamodb.PolicyDynamoRepository;
import com.acme.insurance.policy.infra.dynamodb.mapper.PolicyItemMapper;
import com.acme.insurance.policy.infra.messaging.PolicyRequestPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class PolicyServiceImpl implements PolicyService {

    private final FraudGateway fraudGateway;
    private final PolicyDynamoRepository policyDynamoRepository;
    private final ApiPolicyMapper apiPolicyMapper;
    private final PolicyItemMapper policyItemMapper;
    private final PolicyRequestPublisher policyRequestPublisher;

    public PolicyServiceImpl(FraudGateway fraudGateway,
                             PolicyDynamoRepository policyDynamoRepository,
                             ApiPolicyMapper apiPolicyMapper,
                             PolicyItemMapper policyItemMapper,
                             PolicyRequestPublisher policyRequestPublisher) {
        this.fraudGateway = fraudGateway;
        this.policyDynamoRepository = policyDynamoRepository;
        this.apiPolicyMapper = apiPolicyMapper;
        this.policyItemMapper = policyItemMapper;
        this.policyRequestPublisher = policyRequestPublisher;
    }

    @Override
    public PolicyResponseDto createPolicy(PolicyRequestDto request) {

        Policy policy = apiPolicyMapper.toDomain(request);

        policy = new Policy(
                UUID.randomUUID(),
                policy.customerId(),
                policy.productId(),
                policy.category(),
                policy.salesChannel(),
                policy.paymentMethod(),
                policy.status(),
                policy.createdAt(),
                null,
                policy.coverages(),
                policy.assistances(),
                policy.totalMonthlyPremiumAmount(),
                policy.insuredAmount(),
                List.of(new Policy.StatusHistory(
                        "RECEIVED",
                        policy.createdAt()))
        );

        policyDynamoRepository.save(policyItemMapper.toItem(policy));

        policyRequestPublisher.publish(new PolicyRequestCreatedEvent(
                policy.id(),
                policy.customerId(),
                policy.productId(),
                "RECEIVED",
                Instant.now()
        ));

        FraudAnalysisResponse fraud = fraudGateway.analyze(policy.id(), policy.customerId());

        FraudClassification classification = FraudClassification.from(fraud.classification());

        boolean approved = FraudRules.isApproved(classification, policy.category(), policy.insuredAmount());

        if (approved) {
            var validatedAt = OffsetDateTime.now(ZoneOffset.UTC);
            policy = policy.withStatusAndHistory("VALIDATED", validatedAt);
            policyDynamoRepository.save(policyItemMapper.toItem(policy));
            policyRequestPublisher.publish(new PolicyRequestStatusChangedEvent(
                    policy.id(),
                    policy.customerId(),
                    policy.productId(),
                    policy.status(),
                    classification.name(),
                    validatedAt.toInstant()
            ));

            var pendingAt = OffsetDateTime.now(ZoneOffset.UTC);
            policy = policy.withStatusAndHistory("PENDING", pendingAt);
            policyDynamoRepository.save(policyItemMapper.toItem(policy));
            policyRequestPublisher.publish(new PolicyRequestStatusChangedEvent(
                    policy.id(),
                    policy.customerId(),
                    policy.productId(),
                    policy.status(),
                    classification.name(),
                    pendingAt.toInstant()
            ));

        } else {
            var rejectedAt = OffsetDateTime.now(ZoneOffset.UTC);
            policy = policy.withStatusAndHistory("REJECTED", rejectedAt);
            policyDynamoRepository.save(policyItemMapper.toItem(policy));
            policyRequestPublisher.publish(new PolicyRequestStatusChangedEvent(
                    policy.id(),
                    policy.customerId(),
                    policy.productId(),
                    policy.status(),
                    classification.name(),
                    rejectedAt.toInstant()
            ));
        }

        return apiPolicyMapper.toResponse(policy);
    }

    @Override
    public PolicyResponseDto getPolicyById(UUID id) {
        return policyDynamoRepository.findById(id.toString())
                .map(policyItemMapper::toDomain)
                .map(apiPolicyMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    public List<PolicyResponseDto> getPoliciesByCustomerId(UUID customerId) {
        return policyDynamoRepository.findByCustomerId(customerId.toString()).stream()
                .map(policyItemMapper::toDomain)
                .map(apiPolicyMapper::toResponse)
                .toList();
    }
}
