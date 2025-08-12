package com.acme.insurance.policy.app.dto.fraud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FraudAnalysisResponseTest {

    private static ObjectMapper mapper() {
        var m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }

    @Test
    void shouldSerializeToExpectedJson() throws Exception {
        var analyzedAt = OffsetDateTime.parse("2025-08-12T16:00:00Z");
        var occAt = OffsetDateTime.parse("2025-08-11T10:30:00Z");

        var dto = new FraudAnalysisResponse(
                "ord-1", "cust-1", analyzedAt, "HIGH_RISK",
                List.of(new FraudAnalysisResponse.Occurrence(
                        "occ-1", 123L, "SUSPECT", "desc", occAt, occAt))
        );

        var json = mapper().writeValueAsString(dto);

        assertThat(json).contains(
                "\"orderId\":\"ord-1\"",
                "\"customerId\":\"cust-1\"",
                "\"classification\":\"HIGH_RISK\"",
                "\"analyzedAt\":\"2025-08-12T16:00:00Z\"",
                "\"occurrences\":[",
                "\"id\":\"occ-1\"",
                "\"productId\":123",
                "\"type\":\"SUSPECT\"",
                "\"description\":\"desc\"",
                "\"createdAt\":\"2025-08-11T10:30:00Z\"",
                "\"updatedAt\":\"2025-08-11T10:30:00Z\""
        );
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        var json = """
        {
          "orderId":"ord-1",
          "customerId":"cust-1",
          "analyzedAt":"2025-08-12T16:00Z",
          "classification":"NO_INFO",
          "occurrences":[
            {
              "id":"occ-1",
              "productId":123,
              "type":"SUSPECT",
              "description":"desc",
              "createdAt":"2025-08-11T10:30Z",
              "updatedAt":"2025-08-11T10:30Z"
            }
          ]
        }
        """;

        var obj = mapper().readValue(json, FraudAnalysisResponse.class);

        assertThat(obj.orderId()).isEqualTo("ord-1");
        assertThat(obj.customerId()).isEqualTo("cust-1");
        assertThat(obj.classification()).isEqualTo("NO_INFO");
        assertThat(obj.analyzedAt().toString()).isEqualTo("2025-08-12T16:00Z");
        assertThat(obj.occurrences()).hasSize(1);
        var occ = obj.occurrences().get(0);
        assertThat(occ.id()).isEqualTo("occ-1");
        assertThat(occ.productId()).isEqualTo(123L);
        assertThat(occ.type()).isEqualTo("SUSPECT");
        assertThat(occ.description()).isEqualTo("desc");
        assertThat(occ.createdAt().toString()).isEqualTo("2025-08-11T10:30Z");
        assertThat(occ.updatedAt().toString()).isEqualTo("2025-08-11T10:30Z");
    }
}