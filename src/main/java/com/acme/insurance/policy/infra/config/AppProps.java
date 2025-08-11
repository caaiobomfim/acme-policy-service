package com.acme.insurance.policy.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public class AppProps {

    public record Aws(
            String region,
            Credentials credentials,
            Endpoints endpoints
    ) {
        public record Credentials(String accessKey, String secretKey) {}
        public record Endpoints(String sqs, String dynamodb) {}
    }

    public record Sqs(
            Queues queues,
            Listener listener
    ) {
        public record Queues(String payments, String subscriptions, String orders) {}
        public record Listener(
                @DefaultValue("10") Integer maxMessages,
                @DefaultValue("2s") String pollTimeout,
                @DefaultValue("30s") String visibilityTimeout,
                @DefaultValue("2") Integer concurrency
        ) {}
    }

    public record Dynamodb(
            String table,
            Indexes indexes
    ) {
        public record Indexes(String customer) {}
    }

    private final Aws aws;
    private final Sqs sqs;
    private final Dynamodb dynamodb;

    public AppProps(Aws aws, Sqs sqs, Dynamodb dynamodb) {
        this.aws = aws;
        this.sqs = sqs;
        this.dynamodb = dynamodb;
    }

    public Aws aws() {
        return aws;
    }

    public Sqs sqs() {
        return sqs;
    }

    public Dynamodb dynamodb() {
        return dynamodb;
    }
}
