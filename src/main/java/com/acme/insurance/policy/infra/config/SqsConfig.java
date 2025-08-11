package com.acme.insurance.policy.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(AppProps.class)
public class SqsConfig {

    @Bean
    SqsAsyncClient sqsAsyncClient(AppProps props) {
        var aws = props.aws();
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(aws.endpoints().sqs()))
                .region(Region.of(aws.region()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        aws.credentials().accessKey(),
                                        aws.credentials().secretKey())))
                .build();
    }
}
