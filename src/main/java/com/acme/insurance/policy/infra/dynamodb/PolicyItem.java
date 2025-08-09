package com.acme.insurance.policy.infra.dynamodb;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@DynamoDbBean
public class PolicyItem{
    private String policyId;
    private String customerId;
    private String productId;
    private String category;
    private String salesChannel;
    private String paymentMethod;
    private String status;
    private String createdAt;
    private String finishedAt;
    private Map<String, String> coverages;
    private List<String> assistances;
    private String totalMonthlyPremiumAmount;
    private String insuredAmount;
    private List<Map<String, String>> history;

    @DynamoDbPartitionKey
    public String getPolicyId() {
        return policyId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi_customer")
    public String getCustomerId() {
        return customerId;
    }
}


