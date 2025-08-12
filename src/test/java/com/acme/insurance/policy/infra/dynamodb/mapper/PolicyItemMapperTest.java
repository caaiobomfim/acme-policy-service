package com.acme.insurance.policy.infra.dynamodb.mapper;

import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import com.acme.insurance.policy.infra.dynamodb.PolicyItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyItemMapperTest {

    private final PolicyItemMapper mapper = Mappers.getMapper(PolicyItemMapper.class);

    private Policy sampleDomain() {
        UUID id = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        OffsetDateTime created = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        OffsetDateTime finished = OffsetDateTime.parse("2024-01-03T12:34:56Z");

        return new Policy(
                id,
                customer,
                product,
                "AUTO",
                "ONLINE",
                "CREDIT_CARD",
                PolicyStatus.APPROVED,
                created,
                finished,
                Map.of("COLLISION", new BigDecimal("123.45")),
                List.of(),
                new BigDecimal("150.00"),
                new BigDecimal("10000"),
                List.of(
                        new Policy.StatusHistory(PolicyStatus.RECEIVED, OffsetDateTime.parse("2024-01-01T00:00:00Z")),
                        new Policy.StatusHistory(PolicyStatus.APPROVED, OffsetDateTime.parse("2024-01-02T00:00:00Z"))
                )
        );
    }

    private PolicyItem sampleItem() {
        PolicyItem it = new PolicyItem();
        it.setPolicyId(UUID.randomUUID().toString());
        it.setCustomerId(UUID.randomUUID().toString());
        it.setProductId(UUID.randomUUID().toString());
        it.setCategory("RESIDENTIAL");
        it.setSalesChannel("AGENCY");
        it.setPaymentMethod("PIX");
        it.setStatus("RECEIVED");
        it.setCreatedAt("2024-02-10T10:20:30Z");
        it.setFinishedAt("2024-02-12T08:00:00Z");
        it.setCoverages(Map.of("FIRE", "9999.99"));
        it.setAssistances(List.of());
        it.setTotalMonthlyPremiumAmount("321.00");
        it.setInsuredAmount("750000");
        it.setHistory(List.of(
                Map.of("status", "RECEIVED", "timestamp", "2024-02-10T10:20:30Z"),
                Map.of("status", "APPROVED", "timestamp", "2024-02-11T10:20:30Z")
        ));
        return it;
    }

    @Test
    @DisplayName("toItem: mapeia Domain -> PolicyItem convertendo UUID/OffsetDateTime/BigDecimal/Maps/History")
    void toItem_maps_all_core_fields() {
        Policy domain = sampleDomain();

        PolicyItem out = mapper.toItem(domain);

        assertThat(out.getPolicyId()).isEqualTo(domain.id().toString());
        assertThat(out.getCustomerId()).isEqualTo(domain.customerId().toString());
        assertThat(out.getProductId()).isEqualTo(domain.productId().toString());

        assertThat(out.getCategory()).isEqualTo(domain.category());
        assertThat(out.getSalesChannel()).isEqualTo(domain.salesChannel());
        assertThat(out.getPaymentMethod()).isEqualTo(domain.paymentMethod());
        assertThat(out.getStatus()).isEqualTo(domain.status().name());

        assertThat(OffsetDateTime.parse(out.getCreatedAt())).isEqualTo(domain.createdAt());
        assertThat(OffsetDateTime.parse(out.getFinishedAt())).isEqualTo(domain.finishedAt());

        assertThat(out.getTotalMonthlyPremiumAmount()).isEqualTo(domain.totalMonthlyPremiumAmount().toPlainString());
        assertThat(out.getInsuredAmount()).isEqualTo(domain.insuredAmount().toPlainString());

        assertThat(out.getCoverages()).containsEntry("COLLISION", "123.45");

        assertThat(out.getHistory()).hasSize(2);
        assertThat(out.getHistory().get(0)).containsEntry("status", "RECEIVED");
        assertThat(OffsetDateTime.parse(out.getHistory().get(0).get("timestamp")))
                .isEqualTo(OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        assertThat(out.getHistory().get(1)).containsEntry("status", "APPROVED");
        assertThat(OffsetDateTime.parse(out.getHistory().get(1).get("timestamp")))
                .isEqualTo(OffsetDateTime.parse("2024-01-02T00:00:00Z"));
    }

    @Test
    @DisplayName("toDomain: mapeia PolicyItem -> Domain convertendo String/ISO/Maps/History")
    void toDomain_maps_all_core_fields() {
        PolicyItem item = sampleItem();

        Policy out = mapper.toDomain(item);

        assertThat(out.id().toString()).isEqualTo(item.getPolicyId());
        assertThat(out.customerId().toString()).isEqualTo(item.getCustomerId());
        assertThat(out.productId().toString()).isEqualTo(item.getProductId());

        assertThat(out.category()).isEqualTo(item.getCategory());
        assertThat(out.salesChannel()).isEqualTo(item.getSalesChannel());
        assertThat(out.paymentMethod()).isEqualTo(item.getPaymentMethod());
        assertThat(out.status().name()).isEqualTo(item.getStatus());

        assertThat(out.createdAt()).isEqualTo(OffsetDateTime.parse("2024-02-10T10:20:30Z"));
        assertThat(out.finishedAt()).isEqualTo(OffsetDateTime.parse("2024-02-12T08:00:00Z"));

        assertThat(out.totalMonthlyPremiumAmount()).isEqualByComparingTo("321.00");
        assertThat(out.insuredAmount()).isEqualByComparingTo("750000");

        assertThat(out.coverages()).containsEntry("FIRE", new BigDecimal("9999.99"));

        assertThat(out.history()).hasSize(2);
        assertThat(out.history().get(0).status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(out.history().get(0).timestamp()).isEqualTo(OffsetDateTime.parse("2024-02-10T10:20:30Z"));
        assertThat(out.history().get(1).status()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(out.history().get(1).timestamp()).isEqualTo(OffsetDateTime.parse("2024-02-11T10:20:30Z"));
    }

    @Test
    @DisplayName("Round-trip Domain -> Item -> Domain preserva valores relevantes")
    void roundTrip_domain_item_domain() {
        Policy original = sampleDomain();

        PolicyItem item = mapper.toItem(original);
        Policy back = mapper.toDomain(item);

        assertThat(back.id()).isEqualTo(original.id());
        assertThat(back.customerId()).isEqualTo(original.customerId());
        assertThat(back.productId()).isEqualTo(original.productId());
        assertThat(back.category()).isEqualTo(original.category());
        assertThat(back.salesChannel()).isEqualTo(original.salesChannel());
        assertThat(back.paymentMethod()).isEqualTo(original.paymentMethod());
        assertThat(back.status()).isEqualTo(original.status());
        assertThat(back.createdAt()).isEqualTo(original.createdAt());
        assertThat(back.finishedAt()).isEqualTo(original.finishedAt());

        assertThat(back.totalMonthlyPremiumAmount()).isEqualByComparingTo(original.totalMonthlyPremiumAmount());
        assertThat(back.insuredAmount()).isEqualByComparingTo(original.insuredAmount());
        assertThat(back.coverages()).usingRecursiveComparison().isEqualTo(original.coverages());

        assertThat(back.history()).hasSize(original.history().size());
        for (int i = 0; i < back.history().size(); i++) {
            assertThat(back.history().get(i).status()).isEqualTo(original.history().get(i).status());
            assertThat(back.history().get(i).timestamp()).isEqualTo(original.history().get(i).timestamp());
        }
    }

    @Test
    @DisplayName("Round-trip Item -> Domain -> Item preserva valores relevantes")
    void roundTrip_item_domain_item() {
        PolicyItem original = sampleItem();

        Policy domain = mapper.toDomain(original);
        PolicyItem back = mapper.toItem(domain);

        assertThat(back.getPolicyId()).isEqualTo(original.getPolicyId());
        assertThat(back.getCustomerId()).isEqualTo(original.getCustomerId());
        assertThat(back.getProductId()).isEqualTo(original.getProductId());
        assertThat(back.getCategory()).isEqualTo(original.getCategory());
        assertThat(back.getSalesChannel()).isEqualTo(original.getSalesChannel());
        assertThat(back.getPaymentMethod()).isEqualTo(original.getPaymentMethod());
        assertThat(back.getStatus()).isEqualTo(original.getStatus());

        assertThat(OffsetDateTime.parse(back.getCreatedAt()))
                .isEqualTo(OffsetDateTime.parse(original.getCreatedAt()));
        assertThat(OffsetDateTime.parse(back.getFinishedAt()))
                .isEqualTo(OffsetDateTime.parse(original.getFinishedAt()));

        assertThat(new BigDecimal(back.getTotalMonthlyPremiumAmount()))
                .isEqualByComparingTo(new BigDecimal(original.getTotalMonthlyPremiumAmount()));
        assertThat(new BigDecimal(back.getInsuredAmount()))
                .isEqualByComparingTo(new BigDecimal(original.getInsuredAmount()));
        assertThat(back.getCoverages()).isEqualTo(original.getCoverages());

        assertThat(back.getHistory()).isEqualTo(original.getHistory());
    }
}