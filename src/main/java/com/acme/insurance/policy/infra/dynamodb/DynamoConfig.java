package com.acme.insurance.policy.infra.dynamodb;

import com.acme.insurance.policy.infra.config.AppProps;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(AppProps.class)
public class DynamoConfig {

    @Bean
    DynamoDbClient dynamoDbClient(AppProps props) {
        var aws = props.aws();
        return DynamoDbClient.builder()
                .region(Region.of(aws.region()))
                .endpointOverride(URI.create(aws.endpoints().dynamodb()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        aws.credentials().accessKey(),
                                        aws.credentials().secretKey())))
                .build();
    }
}
