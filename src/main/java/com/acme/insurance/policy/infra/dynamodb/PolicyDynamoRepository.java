package com.acme.insurance.policy.infra.dynamodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PolicyDynamoRepository.class);

    private final DynamoDbTable<PolicyItem> table;

    public PolicyDynamoRepository(DynamoDbClient dynamoDbClient,
                                  @Value("${aws.dynamodb.table}") String tableName) {
        log.info("[DynamoDB] Inicializando repositório para a tabela: {}", tableName);
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(PolicyItem.class));
    }

    public void save(PolicyItem item) {
        log.info("[DynamoDB] Salvando PolicyItem - policyId={} customerId={}",
                item.getPolicyId(), item.getCustomerId());
        table.putItem(item);
        log.info("[DynamoDB] PolicyItem salvo com sucesso - policyId={}", item.getPolicyId());
    }

    public Optional<PolicyItem> findById(String policyId) {
        log.info("[DynamoDB] Buscando PolicyItem por policyId={}", policyId);
        PolicyItem key = new PolicyItem();
        key.setPolicyId(policyId);
        Optional<PolicyItem> result = Optional.ofNullable(table.getItem(key));
        log.info("[DynamoDB] Resultado da busca por policyId={}: {}", policyId,
                result.isPresent() ? "ENCONTRADO" : "NÃO ENCONTRADO");
        return result;
    }

    public List<PolicyItem> findByCustomerId(String customerId) {
        log.info("[DynamoDB] Buscando PolicyItems por customerId={}", customerId);
        var result = table.index("gsi_customer")
                .query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue(customerId))
                ));
        List<PolicyItem> items = result.stream()
                .flatMap(page -> page.items().stream())
                .toList();
        log.info("[DynamoDB] Encontrados {} registros para customerId={}", items.size(), customerId);
        return items;
    }
}
