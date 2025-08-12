package com.acme.insurance.policy.infra.dynamodb.mapper;

import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.acme.insurance.policy.infra.dynamodb.mapper.PolicyMapConverters.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyMapConvertersTest {

    @Test
    @DisplayName("UUID <-> String: round-trip e null")
    void uuid_roundTrip_and_null() {
        UUID id = UUID.randomUUID();
        String s = uuidToString(id);
        assertThat(s).isEqualTo(id.toString());
        assertThat(stringToUuid(s)).isEqualTo(id);

        assertThat(uuidToString(null)).isNull();
        assertThat(stringToUuid(null)).isNull();

        assertThatThrownBy(() -> stringToUuid("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OffsetDateTime <-> String: round-trip e null")
    void odt_roundTrip_and_null() {
        OffsetDateTime odt = OffsetDateTime.parse("2024-01-01T12:34:56Z");
        String s = odtToString(odt);
        assertThat(s).isEqualTo("2024-01-01T12:34:56Z");
        assertThat(stringToOdt(s)).isEqualTo(odt);

        assertThat(odtToString(null)).isNull();
        assertThat(stringToOdt(null)).isNull();
    }

    @Test
    @DisplayName("BigDecimal <-> String (toPlainString): round-trip e null")
    void bd_roundTrip_and_null() {
        BigDecimal bd1 = new BigDecimal("123.4500");
        assertThat(bdToString(bd1)).isEqualTo("123.4500");
        assertThat(stringToBd("123.4500")).isEqualByComparingTo(bd1);

        BigDecimal sci = new BigDecimal("1E+6");
        assertThat(bdToString(sci)).isEqualTo("1000000");
        assertThat(stringToBd("1000000")).isEqualByComparingTo(sci);

        assertThat(bdToString(null)).isNull();
        assertThat(stringToBd(null)).isNull();
    }

    @Test
    @DisplayName("Map<String, BigDecimal> <-> Map<String, String>: round-trip, null e vazio")
    void map_roundTrip() {
        Map<String, BigDecimal> in = Map.of(
                "a", new BigDecimal("10.00"),
                "b", new BigDecimal("1E+3")
        );
        Map<String, String> asStr = toStringMap(in);
        assertThat(asStr).containsEntry("a", "10.00")
                .containsEntry("b", "1000");

        Map<String, BigDecimal> back = toBigDecimalMap(asStr);
        assertThat(back).hasSize(2);
        assertThat(back.get("a")).isEqualByComparingTo("10.00");
        assertThat(back.get("b")).isEqualByComparingTo("1000");

        assertThat(toStringMap(null)).isEmpty();
        assertThat(toBigDecimalMap(null)).isEmpty();
        assertThat(toStringMap(Map.of())).isEmpty();
        assertThat(toBigDecimalMap(Map.of())).isEmpty();
    }

    @Test
    @DisplayName("History -> List<Map> e volta: round-trip preservando ordem")
    void history_roundTrip_preserves_order() {
        OffsetDateTime t1 = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        OffsetDateTime t2 = OffsetDateTime.parse("2024-01-02T00:00:00Z");
        List<Policy.StatusHistory> hist = List.of(
                new Policy.StatusHistory(PolicyStatus.RECEIVED, t1),
                new Policy.StatusHistory(PolicyStatus.APPROVED, t2)
        );

        List<Map<String, String>> maps = toHistoryMap(hist);
        assertThat(maps).hasSize(2);

        assertThat(maps.get(0)).containsEntry("status", "RECEIVED");
        assertThat(maps.get(0).get("timestamp")).isEqualTo(t1.toString());

        assertThat(maps.get(1)).containsEntry("status", "APPROVED");
        assertThat(maps.get(1).get("timestamp")).isEqualTo(t2.toString());

        List<Policy.StatusHistory> back = fromHistoryMap(maps);
        assertThat(back).hasSize(2);
        assertThat(back.get(0).status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(back.get(0).timestamp()).isEqualTo(t1);
        assertThat(back.get(1).status()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(back.get(1).timestamp()).isEqualTo(t2);
    }

    @Test
    @DisplayName("History null/empty -> lista vazia")
    void history_null_empty() {
        assertThat(toHistoryMap(null)).isEmpty();
        assertThat(toHistoryMap(List.of())).isEmpty();

        assertThat(fromHistoryMap(null)).isEmpty();
        assertThat(fromHistoryMap(List.of())).isEmpty();
    }
}