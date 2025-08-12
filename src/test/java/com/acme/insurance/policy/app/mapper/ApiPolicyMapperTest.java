package com.acme.insurance.policy.app.mapper;

import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiPolicyMapperTest {

    private final ApiPolicyMapper mapper = Mappers.getMapper(ApiPolicyMapper.class);

    private Policy sampleDomain() {
        UUID id = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        UUID product = UUID.randomUUID();

        return new Policy(
                id,
                customer,
                product,
                "AUTO",
                "ONLINE",
                "CREDIT_CARD",
                PolicyStatus.APPROVED,
                OffsetDateTime.parse("2024-05-01T10:00:00Z"),
                OffsetDateTime.parse("2024-05-02T12:00:00Z"),
                Map.of("COLLISION", new BigDecimal("123.45")),
                List.of("TOWING"),
                new BigDecimal("150.00"),
                new BigDecimal("10000"),
                List.of(
                        new Policy.StatusHistory(PolicyStatus.RECEIVED, OffsetDateTime.parse("2024-05-01T10:00:00Z")),
                        new Policy.StatusHistory(PolicyStatus.APPROVED, OffsetDateTime.parse("2024-05-01T10:05:00Z"))
                )
        );
    }

    @Test
    @DisplayName("toDomain: aplica defaults (status=RECEIVED, createdAt≈now) e copia campos do request")
    void toDomain_setsDefaults_andCopies() {
        UUID customerId = UUID.randomUUID();
        UUID productId  = UUID.randomUUID();

        PolicyRequestDto req = org.instancio.Instancio.of(PolicyRequestDto.class)
                .set(org.instancio.Select.field("customerId"), customerId)
                .set(org.instancio.Select.field("productId"),  productId)
                .set(org.instancio.Select.field("category"),   "AUTO")
                .set(org.instancio.Select.field("salesChannel"), "ONLINE")
                .set(org.instancio.Select.field("paymentMethod"), "CREDIT_CARD")
                .set(org.instancio.Select.field("coverages"), Map.of("X", new BigDecimal("10.00")))
                .set(org.instancio.Select.field("assistances"), List.of("TOWING"))
                .set(org.instancio.Select.field("totalMonthlyPremiumAmount"), new BigDecimal("150.00"))
                .set(org.instancio.Select.field("insuredAmount"), new BigDecimal("10000"))
                .create();

        OffsetDateTime before = OffsetDateTime.now();
        Policy out = mapper.toDomain(req);
        OffsetDateTime after  = OffsetDateTime.now();

        assertThat(out.id()).as("id é ignorado pelo mapper").isNull();
        assertThat(out.status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(out.finishedAt()).isNull();
        assertThat(out.history()).as("history é ignorado").isNull();

        assertThat(out.createdAt()).isNotNull();
        assertThat(Math.abs(ChronoUnit.SECONDS.between(out.createdAt(), before))).isLessThanOrEqualTo(2);
        assertThat(Math.abs(ChronoUnit.SECONDS.between(out.createdAt(), after))).isLessThanOrEqualTo(2);

        assertThat(out.customerId()).isEqualTo(customerId);
        assertThat(out.productId()).isEqualTo(productId);
        assertThat(out.category()).isEqualTo("AUTO");
        assertThat(out.salesChannel()).isEqualTo("ONLINE");
        assertThat(out.paymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(out.coverages()).containsEntry("X", new BigDecimal("10.00"));
        assertThat(out.assistances()).contains("TOWING");
        assertThat(out.totalMonthlyPremiumAmount()).isEqualByComparingTo("150.00");
        assertThat(out.insuredAmount()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("toResponse: mapeia 1–1 e converte history -> StatusHistoryDto preservando ordem")
    void toResponse_mapsAll_andHistory() {
        Policy domain = sampleDomain();

        PolicyResponseDto dto = mapper.toResponse(domain);

        assertThat(dto.id()).isEqualTo(domain.id());
        assertThat(dto.customerId()).isEqualTo(domain.customerId());
        assertThat(dto.productId()).isEqualTo(domain.productId());
        assertThat(dto.category()).isEqualTo(domain.category());
        assertThat(dto.salesChannel()).isEqualTo(domain.salesChannel());
        assertThat(dto.paymentMethod()).isEqualTo(domain.paymentMethod());
        assertThat(dto.status()).isEqualTo(domain.status().name());
        assertThat(dto.createdAt()).isEqualTo(domain.createdAt());
        assertThat(dto.finishedAt()).isEqualTo(domain.finishedAt());
        assertThat(dto.totalMonthlyPremiumAmount()).isEqualByComparingTo(domain.totalMonthlyPremiumAmount());
        assertThat(dto.insuredAmount()).isEqualByComparingTo(domain.insuredAmount());
        assertThat(dto.coverages()).isEqualTo(domain.coverages());
        assertThat(dto.assistances()).isEqualTo(domain.assistances());

        assertThat(dto.history()).hasSize(2);
        assertThat(dto.history().get(0).status()).isEqualTo("RECEIVED");
        assertThat(dto.history().get(0).timestamp()).isEqualTo(OffsetDateTime.parse("2024-05-01T10:00:00Z"));
        assertThat(dto.history().get(1).status()).isEqualTo("APPROVED");
        assertThat(dto.history().get(1).timestamp()).isEqualTo(OffsetDateTime.parse("2024-05-01T10:05:00Z"));
    }

    @Test
    @DisplayName("toResponse: history null vira lista vazia (mapHistory)")
    void toResponse_nullHistory_becomesEmptyList() {
        Policy domain = new Policy(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "RESIDENTIAL",
                "AGENCY",
                "PIX",
                PolicyStatus.RECEIVED,
                OffsetDateTime.parse("2024-05-10T09:00:00Z"),
                null,
                Map.of(),
                List.of(),
                new BigDecimal("1.00"),
                new BigDecimal("2.00"),
                null
        );

        PolicyResponseDto dto = mapper.toResponse(domain);

        assertThat(dto.history()).isNotNull().isEmpty();
    }
}