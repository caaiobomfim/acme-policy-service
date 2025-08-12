package com.acme.insurance.policy.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyTest {

    private Policy base(PolicyStatus status, List<Policy.StatusHistory> history) {
        return new Policy(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "AUTO",
                "ONLINE",
                "CREDIT_CARD",
                status,
                OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                null,
                Map.of("COLLISION", new BigDecimal("10000")),
                List.of("TOWING"),
                new BigDecimal("150.00"),
                new BigDecimal("50000"),
                history
        );
    }

    @Test
    @DisplayName("withStatusAndHistory: adiciona entrada, não muta o original e history é imutável")
    void withStatusAndHistory_appends_and_isImmutable() {
        var t0 = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        var t1 = OffsetDateTime.parse("2024-01-02T00:00:00Z");

        var origHist = List.of(new Policy.StatusHistory(PolicyStatus.RECEIVED, t0));
        var p = base(PolicyStatus.RECEIVED, origHist);

        var out = p.withStatusAndHistory(PolicyStatus.APPROVED, t1);

        assertThat(out).isNotSameAs(p);
        assertThat(out.status()).isEqualTo(PolicyStatus.APPROVED);

        assertThat(out.history()).hasSize(2);
        assertThat(out.history().get(0).status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(out.history().get(0).timestamp()).isEqualTo(t0);
        assertThat(out.history().get(1).status()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(out.history().get(1).timestamp()).isEqualTo(t1);

        assertThat(p.history()).hasSize(1);
        assertThat(p.status()).isEqualTo(PolicyStatus.RECEIVED);

        assertThatThrownBy(() -> out.history().add(new Policy.StatusHistory(PolicyStatus.CANCELLED, t1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("withStatusAndHistory: quando history = null, inicia lista com 1 entrada")
    void withStatusAndHistory_nullHistory_startsList() {
        var t = OffsetDateTime.parse("2024-01-03T00:00:00Z");
        var p = base(PolicyStatus.RECEIVED, null);

        var out = p.withStatusAndHistory(PolicyStatus.APPROVED, t);

        assertThat(out.history()).hasSize(1);
        assertThat(out.history().get(0).status()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(out.history().get(0).timestamp()).isEqualTo(t);
    }

    @Test
    @DisplayName("withFinishedAt: atualiza finishedAt preservando os demais campos e referências de coleções")
    void withFinishedAt_setsOnlyFinishedAt() {
        var p = base(PolicyStatus.RECEIVED, List.of());
        var when = OffsetDateTime.parse("2024-01-05T12:00:00Z");

        var out = p.withFinishedAt(when);

        assertThat(out).isNotSameAs(p);
        assertThat(out.finishedAt()).isEqualTo(when);
        assertThat(p.finishedAt()).isNull();

        assertThat(out.id()).isEqualTo(p.id());
        assertThat(out.customerId()).isEqualTo(p.customerId());
        assertThat(out.productId()).isEqualTo(p.productId());
        assertThat(out.category()).isEqualTo(p.category());
        assertThat(out.coverages()).isSameAs(p.coverages());
        assertThat(out.assistances()).isSameAs(p.assistances());
        assertThat(out.status()).isEqualTo(p.status());
        assertThat(out.history()).isEqualTo(p.history());
    }

    @Test
    @DisplayName("isFinalStatus: null -> false; APPROVED/REJECTED/CANCELLED -> true; RECEIVED -> false")
    void isFinalStatus_cases() {
        var pNull = base(null, List.of());
        assertThat(pNull.isFinalStatus()).isFalse();

        assertThat(base(PolicyStatus.APPROVED, List.of()).isFinalStatus()).isTrue();
        assertThat(base(PolicyStatus.REJECTED, List.of()).isFinalStatus()).isTrue();
        assertThat(base(PolicyStatus.CANCELLED, List.of()).isFinalStatus()).isTrue();

        assertThat(base(PolicyStatus.RECEIVED, List.of()).isFinalStatus()).isFalse();
    }
}