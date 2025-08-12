package com.acme.insurance.policy.infra.dynamodb;

import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import com.acme.insurance.policy.support.AwsIntegrationBase;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.cloud.aws.sqs.enabled=false",
        "app.aws.sqs.enabled=false",
        "app.aws.sqs.listeners.enabled=false"
})
@Tag("integration")
class PolicyDynamoRepositoryIT extends AwsIntegrationBase {

    @MockBean
    SqsTemplate sqsTemplate;

    @Autowired
    PolicyDynamoRepository repo;

    @TestConfiguration
    static class DynamoTestConfig {
        @Bean @Primary
        DynamoDbClient testDynamoDbClient() {
            var edge = URI.create("http://localhost:" + LOCALSTACK.getMappedPort(4566));
            return DynamoDbClient.builder()
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .endpointOverride(edge)
                    .region(software.amazon.awssdk.regions.Region.of(LOCALSTACK.getRegion()))
                    .credentialsProvider(
                            software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                                    software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                                            LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                    .build();
        }

        @Bean @Primary
        DynamoDbEnhancedClient testEnhanced(DynamoDbClient c) {
            return DynamoDbEnhancedClient.builder().dynamoDbClient(c).build();
        }
    }

    @Test
    void deveSalvarERecuperarPolicy() {
        var id = UUID.randomUUID();
        var p = new Policy(
                id,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "AUTO",
                "WEB",
                "CREDIT_CARD",
                PolicyStatus.PENDING,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                Map.of("COLLISION", new BigDecimal("10000")),
                List.of("TOWING"),
                new BigDecimal("99.90"),
                new BigDecimal("50000"),
                new ArrayList<>()
        );

        repo.save(p);

        var loaded = repo.findById(id);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().status()).isEqualTo(PolicyStatus.PENDING);
    }
}
