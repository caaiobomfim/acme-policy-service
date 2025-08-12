package com.acme.insurance.policy.infra.fraud;

import com.acme.insurance.policy.app.dto.fraud.FraudAnalysisResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudGatewayFeignAdapterTest {

    @Mock
    FraudClient client;

    @InjectMocks
    FraudGatewayFeignAdapter adapter;

    @Test
    @DisplayName("analyze() delega ao client com UUIDs em String e devolve a mesma resposta")
    void analyze_delegates_and_returns_same_instance() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        FraudAnalysisResponse response = mock(FraudAnalysisResponse.class);
        when(client.analyze(orderId.toString(), customerId.toString())).thenReturn(response);

        FraudAnalysisResponse out = adapter.analyze(orderId, customerId);

        assertThat(out).isSameAs(response);
        verify(client, times(1)).analyze(orderId.toString(), customerId.toString());
        verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("analyze() propaga exceção do client")
    void analyze_propagates_client_exception() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(client.analyze(anyString(), anyString()))
                .thenThrow(new RuntimeException("fraud api down"));

        assertThatThrownBy(() -> adapter.analyze(orderId, customerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fraud api down");

        verify(client).analyze(orderId.toString(), customerId.toString());
        verifyNoMoreInteractions(client);
    }
}