package com.acme.insurance.policy.infra.messaging;

import com.acme.insurance.policy.domain.events.PolicyRequestCreatedEvent;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import com.acme.insurance.policy.domain.ports.out.PolicyRequestPublisher;
import com.acme.insurance.policy.support.AwsIntegrationBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@Testcontainers
@SpringBootTest
@Tag("integration")
class OrdersPublisherIT {

    private static final String ORDERS_QUEUE = "orders-topic";

    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:4.7"))
            .withServices(SQS);

    private static SqsClient sqs;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.credentials.access-key", LOCALSTACK::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", LOCALSTACK::getSecretKey);
        registry.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);

        registry.add("spring.cloud.aws.endpoint",
                () -> LOCALSTACK.getEndpointOverride(SQS).toString());
        registry.add("spring.cloud.aws.sqs.endpoint",
                () -> LOCALSTACK.getEndpointOverride(SQS).toString());

        registry.add("app.sqs.enabled", () -> "true");
        registry.add("app.sqs.listeners.enabled", () -> "false");

        registry.add("app.sqs.orders.queue-name", () -> ORDERS_QUEUE);
    }

    @BeforeAll
    static void setUp() {
        LOCALSTACK.start();

        sqs = SqsClient.builder()
                .endpointOverride(LOCALSTACK.getEndpointOverride(SQS))
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .build();

        sqs.createQueue(CreateQueueRequest.builder().queueName(ORDERS_QUEUE).build());
    }

    @AfterAll
    static void tearDown() {
        if (Objects.nonNull(sqs)) sqs.close();
        LOCALSTACK.stop();
    }

    @TestConfiguration
    static class AwsSqsTestConfig {
        @Bean
        @Primary
        SqsAsyncClient sqsAsyncClient() {
            return SqsAsyncClient.builder()
                    .endpointOverride(URI.create(LOCALSTACK.getEndpointOverride(SQS).toString()))
                    .region(Region.of(LOCALSTACK.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                    .build();
        }
    }

    @Autowired
    PolicyRequestPublisher publisher;

    @Test
    void devePublicarEventoNaFilaOrders() {
        var requestId  = UUID.randomUUID();
        var customerId = UUID.randomUUID();
        var productId  = UUID.randomUUID();

        publisher.publish(new PolicyRequestCreatedEvent(
                requestId, customerId, productId, PolicyStatus.PENDING.name(), Instant.now()
        ));

        var queueUrl = sqs.getQueueUrl(b -> b.queueName(ORDERS_QUEUE)).queueUrl();

        var msg = sqs.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(5)
                .maxNumberOfMessages(1)
                .build());

        assertThat(msg.messages()).isNotEmpty();
        assertThat(msg.messages().get(0).body()).contains(requestId.toString());
    }
}
