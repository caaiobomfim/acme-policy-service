package com.acme.insurance.policy.infra.dynamodb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Optional;

@Component
public class PolicyDynamoRepository {

    private final DynamoDbTable<PolicyItem> table;

    public PolicyDynamoRepository(DynamoDbClient dynamoDbClient,
                                  @Value("${aws.dynamodb.table}") String tableName) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(PolicyItem.class));
    }

    public void save(PolicyItem item) {
        table.putItem(item);
    }

    public Optional<PolicyItem> findById(String policyId) {
        PolicyItem key = new PolicyItem();
        key.setPolicyId(policyId);
        return Optional.ofNullable(table.getItem(key));
    }

    public List<PolicyItem> findByCustomerId(String customerId) {
        var result = table.index("gsi_customer")
                .query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue(customerId))
                ));
        return result.stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }
}
