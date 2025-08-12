package com.acme.insurance.policy.application;

import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.app.dto.fraud.FraudAnalysisResponse;
import com.acme.insurance.policy.app.mapper.ApiPolicyMapper;
import com.acme.insurance.policy.domain.fraud.FraudClassification;
import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import com.acme.insurance.policy.domain.ports.out.FraudGateway;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import com.acme.insurance.policy.domain.ports.out.PolicyRequestPublisher;
import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePolicyServiceTest {

    @Mock
    PolicyRepository policyRepository;

    @Mock
    PolicyRequestPublisher policyRequestPublisher;

    @Mock
    FraudGateway fraudGateway;

    @Mock
    PolicyStateMachine policyStateMachine;

    @Mock
    ApiPolicyMapper apiPolicyMapper;

    @InjectMocks
    CreatePolicyService service;

    private PolicyRequestDto mockRequest() {
        return mock(PolicyRequestDto.class);
    }

    private Policy basePolicy() {
        return new Policy(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "AUTO",
                "ONLINE",
                "CREDIT_CARD",
                PolicyStatus.RECEIVED,
                OffsetDateTime.now(),
                null,
                Map.of(),
                List.of(),
                new BigDecimal("150.00"),
                new BigDecimal("10000"),
                List.of()
        );
    }

    @Test
    @DisplayName("Happy path: salva, publica, analisa fraude, roda state machine e mapeia a mesma instância quando findById vazio")
    void execute_happyPath_findByIdEmpty() {
        var req = mockRequest();
        var base = basePolicy();

        when(apiPolicyMapper.toDomain(any())).thenReturn(base);
        doAnswer(inv -> inv.getArgument(0)).when(policyRepository).save(any(Policy.class));
        when(policyRepository.findById(any())).thenReturn(Optional.empty());

        FraudAnalysisResponse fraud = mock(FraudAnalysisResponse.class);
        when(fraud.classification()).thenReturn("LOW_RISK");
        when(fraudGateway.analyze(any(), any())).thenReturn(fraud);

        ArgumentCaptor<Policy> toResponseCap = ArgumentCaptor.forClass(Policy.class);
        when(apiPolicyMapper.toResponse(toResponseCap.capture()))
                .thenAnswer(inv -> {
                    Policy p = toResponseCap.getValue();
                    PolicyResponseDto dto = mock(PolicyResponseDto.class);

                    return dto;
                });

        var out = service.execute(req);
        assertThat(out).isNotNull();

        ArgumentCaptor<Policy> savedCap = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).save(savedCap.capture());
        var saved = savedCap.getValue();

        assertThat(saved.status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(saved.customerId()).isEqualTo(base.customerId());
        assertThat(saved.productId()).isEqualTo(base.productId());
        assertThat(saved.id()).isNotEqualTo(base.id());

        InOrder inOrder = inOrder(policyRepository, policyRequestPublisher, fraudGateway, policyStateMachine, apiPolicyMapper);
        inOrder.verify(policyRepository).save(any(Policy.class));
        inOrder.verify(policyRequestPublisher).publish(any(PolicyRequestCreatedEvent.class));
        inOrder.verify(fraudGateway).analyze(eq(saved.id()), eq(saved.customerId()));
        inOrder.verify(policyStateMachine).onFraud(eq(saved), any(FraudClassification.class), eq(saved.category()), eq(saved.insuredAmount()));
        inOrder.verify(policyRepository).findById(eq(saved.id()));
        inOrder.verify(apiPolicyMapper).toResponse(eq(saved));
    }

    @Test
    @DisplayName("Quando findById retorna presente, o mapper usa a policy persistida (ex.: status APPROVED)")
    void execute_usesPersistedFromRepository() {
        var req = mockRequest();
        var base = basePolicy();

        when(apiPolicyMapper.toDomain(any())).thenReturn(base);
        doAnswer(inv -> inv.getArgument(0)).when(policyRepository).save(any(Policy.class));

        when(policyRepository.findById(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            var persisted = new Policy(
                    id, base.customerId(), base.productId(),
                    base.category(), base.salesChannel(), base.paymentMethod(),
                    PolicyStatus.APPROVED,
                    base.createdAt(), OffsetDateTime.now(),
                    base.coverages(), base.assistances(),
                    base.totalMonthlyPremiumAmount(), base.insuredAmount(),
                    List.of()
            );
            return Optional.of(persisted);
        });

        FraudAnalysisResponse fraud = mock(FraudAnalysisResponse.class);
        when(fraud.classification()).thenReturn("LOW_RISK");
        when(fraudGateway.analyze(any(), any())).thenReturn(fraud);

        ArgumentCaptor<Policy> toResponseCap = ArgumentCaptor.forClass(Policy.class);
        when(apiPolicyMapper.toResponse(toResponseCap.capture()))
                .thenReturn(mock(PolicyResponseDto.class));

        var out = service.execute(req);
        assertThat(out).isNotNull();
        assertThat(toResponseCap.getValue().status()).isEqualTo(PolicyStatus.APPROVED);
    }

    @Test
    @DisplayName("Falha ao publicar evento interrompe o fluxo (não analisa fraude nem mapeia response)")
    void execute_publishFails_shortCircuits() {
        var req = mockRequest();
        when(apiPolicyMapper.toDomain(any())).thenReturn(basePolicy());
        doAnswer(inv -> inv.getArgument(0)).when(policyRepository).save(any(Policy.class));
        doThrow(new RuntimeException("publish down")).when(policyRequestPublisher).publish(any(PolicyRequestCreatedEvent.class));

        assertThatThrownBy(() -> service.execute(req)).isInstanceOf(RuntimeException.class);

        verifyNoInteractions(fraudGateway, policyStateMachine);
        verify(apiPolicyMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Falha na análise de fraude impede state machine e mapeamento para response")
    void execute_analyzeFails() {
        var req = mockRequest();
        when(apiPolicyMapper.toDomain(any())).thenReturn(basePolicy());
        doAnswer(inv -> inv.getArgument(0)).when(policyRepository).save(any(Policy.class));
        when(fraudGateway.analyze(any(), any())).thenThrow(new RuntimeException("fraud api down"));

        assertThatThrownBy(() -> service.execute(req)).isInstanceOf(RuntimeException.class);

        verify(policyRequestPublisher).publish(any(PolicyRequestCreatedEvent.class));
        verify(policyStateMachine, never()).onFraud(any(), any(), any(), any());
        verify(apiPolicyMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Classificação textual é mapeada para o enum usado na state machine")
    void execute_mapsFraudClassificationToEnum() {
        var req = mockRequest();
        var base = basePolicy();

        when(apiPolicyMapper.toDomain(any())).thenReturn(base);
        doAnswer(inv -> inv.getArgument(0)).when(policyRepository).save(any(Policy.class));
        when(policyRepository.findById(any())).thenReturn(Optional.empty());

        FraudAnalysisResponse fraud = mock(FraudAnalysisResponse.class);
        when(fraud.classification()).thenReturn("HIGH_RISK");
        when(fraudGateway.analyze(any(), any())).thenReturn(fraud);

        when(apiPolicyMapper.toResponse(any())).thenReturn(mock(PolicyResponseDto.class));

        ArgumentCaptor<FraudClassification> enumCap = ArgumentCaptor.forClass(FraudClassification.class);

        service.execute(req);

        verify(policyStateMachine).onFraud(any(Policy.class), enumCap.capture(), eq(base.category()), eq(base.insuredAmount()));
        assertThat(enumCap.getValue()).isEqualTo(FraudClassification.HIGH_RISK);
    }
}
