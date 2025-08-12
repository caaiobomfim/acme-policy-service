package com.acme.insurance.policy.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.net.URI;

@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class AwsIntegrationBase {

    @Container
    protected static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.7"))
                    .withEnv("LOCALSTACK_SSL", "0")
                    .withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.DYNAMODB)
                    .waitingFor(Wait.forLogMessage(".*Ready\\.\n", 1))
                    .withStartupAttempts(2)
                    .withReuse(true);

    protected static SqsClient sqs;
    protected static DynamoDbClient dynamo;

    // SQS
    protected static final String ORDERS_QUEUE = "orders-topic";
    protected static final String PAYMENTS_QUEUE = "payments-topic";
    protected static final String SUBSCRIPTIONS_QUEUE = "insurance-subscriptions-topic";

    // Dynamo
    protected static final String POLICY_TABLE   = "PolicyRequests";
    private   static final String POLICY_PK      = "policyId";
    private   static final String CUSTOMER_ATTR  = "customerId";
    private   static final String CUSTOMER_GSI   = "gsi_customer";

    @DynamicPropertySource
    static void awsProps(DynamicPropertyRegistry r) {
        r.add("app.aws.region",              LOCALSTACK::getRegion);
        r.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);
        r.add("spring.cloud.aws.sqs.endpoint", () -> "http://localhost:" + LOCALSTACK.getMappedPort(4566));
        r.add("app.aws.sqs.endpoint",          () -> "http://localhost:" + LOCALSTACK.getMappedPort(4566));
        r.add("app.aws.dynamo.endpoint",       () -> "http://localhost:" + LOCALSTACK.getMappedPort(4566));
    }

    @BeforeAll
    static void initAwsClientsAndInfra() {
        var creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()));
        var edge = URI.create("http://localhost:" + LOCALSTACK.getMappedPort(4566));
        var region = Region.of(LOCALSTACK.getRegion());

        sqs = SqsClient.builder()
                .httpClientBuilder(ApacheHttpClient.builder().expectContinueEnabled(false))
                .endpointOverride(edge)
                .region(region)
                .credentialsProvider(creds)
                .build();

        dynamo = DynamoDbClient.builder()
                .httpClientBuilder(ApacheHttpClient.builder().expectContinueEnabled(false))
                .endpointOverride(edge)
                .region(region)
                .credentialsProvider(creds)
                .build();

        // infra
        waitSqsUp();
        ensureQueue(ORDERS_QUEUE);
        ensureQueue(PAYMENTS_QUEUE);
        ensureQueue(SUBSCRIPTIONS_QUEUE);
        ensurePolicyTable();
    }

    private static void waitSqsUp() {
        int tries = 0;
        while (true) {
            try {
                var name = "__healthcheck__";
                sqs.createQueue(b -> b.queueName(name));
                var url = sqs.getQueueUrl(b -> b.queueName(name)).queueUrl();
                sqs.deleteQueue(b -> b.queueUrl(url));
                return;
            } catch (SqsException e) {
                if (++tries >= 12) throw e;
                try { Thread.sleep(500L * tries); } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void ensureQueue(String name) {
        int attempts = 0;
        while (true) {
            try {
                sqs.getQueueUrl(b -> b.queueName(name));
                return;
            } catch (QueueDoesNotExistException nf) {
                try {
                    sqs.createQueue(b -> b.queueName(name));
                    sqs.getQueueUrl(b -> b.queueName(name));
                    return;
                } catch (SqsException e) { /* retry */ }
            } catch (SqsException e) { /* retry */ }
            if (++attempts >= 12) throw new IllegalStateException("Falha ao garantir fila: " + name);
            try { Thread.sleep(500L * attempts); } catch (InterruptedException ignored) {}
        }
    }

    private static void ensurePolicyTable() {
        try {
            var desc = dynamo.describeTable(b -> b.tableName(POLICY_TABLE)).table();
            var currentPk = desc.keySchema().stream()
                    .filter(k -> k.keyType() == KeyType.HASH)
                    .findFirst().map(KeySchemaElement::attributeName).orElse(null);

            var hasCorrectPk  = POLICY_PK.equals(currentPk);
            var hasCustomerGsi = desc.globalSecondaryIndexes() != null
                    && desc.globalSecondaryIndexes().stream().anyMatch(g -> CUSTOMER_GSI.equals(g.indexName()));

            if (!hasCorrectPk || !hasCustomerGsi) {
                dynamo.deleteTable(b -> b.tableName(POLICY_TABLE));
                dynamo.waiter().waitUntilTableNotExists(b -> b.tableName(POLICY_TABLE));
                createPolicyTable();
            }
        } catch (ResourceNotFoundException e) {
            createPolicyTable();
        }
    }

    private static void createPolicyTable() {
        dynamo.createTable(CreateTableRequest.builder()
                .tableName(POLICY_TABLE)
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(POLICY_PK).attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName(CUSTOMER_ATTR).attributeType(ScalarAttributeType.S).build()
                )
                .keySchema(KeySchemaElement.builder().attributeName(POLICY_PK).keyType(KeyType.HASH).build())
                .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                        .indexName(CUSTOMER_GSI)
                        .keySchema(KeySchemaElement.builder().attributeName(CUSTOMER_ATTR).keyType(KeyType.HASH).build())
                        .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());

        dynamo.waiter().waitUntilTableExists(b -> b.tableName(POLICY_TABLE));
    }
}
