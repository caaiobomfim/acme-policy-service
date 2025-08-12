package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import com.acme.insurance.policy.infra.dynamodb.PolicyDynamoRepository;
import com.acme.insurance.policy.support.AwsIntegrationBase;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
class PaymentResultsConsumerIT extends AwsIntegrationBase {

    @Autowired
    PolicyDynamoRepository repo;

    @TestConfiguration
    static class AwsTestConfig {

        @Bean @Primary
        SqsAsyncClient testSqsAsyncClient() {
            var edge = URI.create("http://localhost:" + LOCALSTACK.getMappedPort(4566));
            return SqsAsyncClient.builder()
                    .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                    .endpointOverride(edge)
                    .region(Region.of(LOCALSTACK.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                    .build();
        }

        @Bean @Primary
        SqsTemplate testSqsTemplate(SqsAsyncClient client) {
            return SqsTemplate.builder().sqsAsyncClient(client).build();
        }

        @Bean @Primary
        DynamoDbClient testDynamoDbClient() {
            var edge = URI.create("http://localhost:" + LOCALSTACK.getMappedPort(4566));
            return DynamoDbClient.builder()
                    .httpClientBuilder(ApacheHttpClient.builder().expectContinueEnabled(false))
                    .endpointOverride(edge)
                    .region(Region.of(LOCALSTACK.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                    .build();
        }

        @Bean @Primary
        DynamoDbEnhancedClient testEnhanced(DynamoDbClient c) {
            return DynamoDbEnhancedClient.builder().dynamoDbClient(c).build();
        }
    }

    @Test
    void pagamentoConfirmadoESubscriptionAutorizadaDevemAprovarPolicy() {
        var id = UUID.randomUUID();

        var policy = new Policy(
                id, UUID.randomUUID(), UUID.randomUUID(),
                "AUTO", "WEB", "CREDIT_CARD",
                PolicyStatus.PENDING,
                OffsetDateTime.now(ZoneOffset.UTC), null,
                Map.of("COLLISION", new BigDecimal("10000")),
                List.of("TOWING"),
                new BigDecimal("99.90"), new BigDecimal("50000"),
                List.of()
        );
        repo.save(policy);

        var paymentsUrl = sqs.getQueueUrl(b -> b.queueName(PAYMENTS_QUEUE)).queueUrl();
        var subsUrl     = sqs.getQueueUrl(b -> b.queueName(SUBSCRIPTIONS_QUEUE)).queueUrl();

        var paymentJson = """
          {"requestId":"%s","status":"CONFIRMED","timestamp":"2025-08-12T01:00:00Z"}
        """.formatted(id);

        var subsJson = """
          {"requestId":"%s","status":"AUTHORIZED","timestamp":"2025-08-12T01:00:02Z"}
        """.formatted(id);

        sqs.sendMessage(b -> b.queueUrl(paymentsUrl).messageBody(paymentJson));
        sqs.sendMessage(b -> b.queueUrl(subsUrl).messageBody(subsJson));

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    var found = repo.findById(id);
                    assertThat(found).isPresent();
                    assertThat(found.get().status()).isEqualTo(PolicyStatus.APPROVED);
                });
    }
}
