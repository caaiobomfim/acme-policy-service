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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PolicyServiceImpl.class);

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

        log.info("[SERVICE] Criando nova policy para customerId={} productId={}", request.customerId(), request.productId());

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

        log.info("Salvando policy no DynamoDB - id={}", policy.id());
        policyDynamoRepository.save(policyItemMapper.toItem(policy));

        log.info("Publicando evento PolicyRequestCreatedEvent - id={}", policy.id());
        policyRequestPublisher.publish(new PolicyRequestCreatedEvent(
                policy.id(),
                policy.customerId(),
                policy.productId(),
                "RECEIVED",
                Instant.now()
        ));

        log.info("Chamando análise de fraude...");
        FraudAnalysisResponse fraud = fraudGateway.analyze(policy.id(), policy.customerId());
        log.info("Resultado da fraude - classification={}", fraud.classification());

        FraudClassification classification = FraudClassification.from(fraud.classification());

        boolean approved = FraudRules.isApproved(classification, policy.category(), policy.insuredAmount());
        log.info("Fraude aprovada? {}", approved);

        if (approved) {
            var validatedAt = OffsetDateTime.now(ZoneOffset.UTC);
            log.info("Status alterado para VALIDATED - id={}", policy.id());
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
            log.info("Status alterado para PENDING - id={}", policy.id());
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
            log.info("Status alterado para REJECTED - id={}", policy.id());
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

        log.info("[SERVICE] Policy criada e processada com sucesso - id={}", policy.id());
        return apiPolicyMapper.toResponse(policy);
    }

    @Override
    public PolicyResponseDto getPolicyById(UUID id) {
        log.info("[SERVICE] Buscando policy por id={}", id);
        return policyDynamoRepository.findById(id.toString())
                .map(policyItemMapper::toDomain)
                .map(apiPolicyMapper::toResponse)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Policy não encontrada - id={}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });
    }

    @Override
    public List<PolicyResponseDto> getPoliciesByCustomerId(UUID customerId) {
        log.info("[SERVICE] Buscando policies para customerId={}", customerId);
        List<PolicyResponseDto> list = policyDynamoRepository.findByCustomerId(customerId.toString()).stream()
                .map(policyItemMapper::toDomain)
                .map(apiPolicyMapper::toResponse)
                .toList();
        log.info("[SERVICE] Encontradas {} policies para customerId={}", list.size(), customerId);
        return list;
    }
}
