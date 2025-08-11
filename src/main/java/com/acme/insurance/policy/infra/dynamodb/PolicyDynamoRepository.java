package com.acme.insurance.policy.infra.dynamodb;

import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.ports.out.PolicyRepository;
import com.acme.insurance.policy.infra.config.AppProps;
import com.acme.insurance.policy.infra.dynamodb.mapper.PolicyItemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PolicyDynamoRepository implements PolicyRepository {

    private static final Logger log = LoggerFactory.getLogger(PolicyDynamoRepository.class);

    private final DynamoDbTable<PolicyItem> table;
    private final String customerGsiName;
    private final PolicyItemMapper policyItemMapper;

    public PolicyDynamoRepository(
            DynamoDbClient dynamoDbClient,
            AppProps props,
            PolicyItemMapper policyItemMapper) {
        String tableName = props.dynamodb().table();
        this.customerGsiName = props.dynamodb().indexes().customer();
        this.policyItemMapper = policyItemMapper;

        log.info("[DynamoDB] Inicializando repositório para a tabela: {} (GSI customer: {})",
                tableName, customerGsiName);
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(PolicyItem.class));
    }

    @Override
    public void save(Policy policy) {
        PolicyItem item = policyItemMapper.toItem(policy);
        log.info("[DynamoDB] Salvando PolicyItem - policyId={} customerId={}",
                item.getPolicyId(), item.getCustomerId());
        table.putItem(item);
        log.info("[DynamoDB] PolicyItem salvo com sucesso - policyId={}", item.getPolicyId());
    }

    @Override
    public Optional<Policy> findById(UUID policyId) {
        String idStr = policyId.toString();
        log.info("[DynamoDB] Buscando PolicyItem por policyId={}", idStr);
        PolicyItem key = new PolicyItem();
        key.setPolicyId(idStr);
        Optional<Policy> result = Optional.ofNullable(table.getItem(key))
                .map(policyItemMapper::toDomain);
        log.info("[DynamoDB] Resultado da busca por policyId={}: {}", idStr,
                result.isPresent() ? "ENCONTRADO" : "NÃO ENCONTRADO");
        return result;
    }

    @Override
    public List<Policy> findByCustomerId(UUID customerId) {
        String customerIdStr = customerId.toString();
        log.info("[DynamoDB] Buscando PolicyItems por customerId={} usando GSI={}", customerIdStr, customerGsiName);
        var pages = table.index(customerGsiName)
                .query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue(customerIdStr))));

        var items = pages.stream()
                .flatMap(p -> p.items().stream())
                .toList();

        log.info("[DynamoDB] Encontrados {} registros para customerId={}", items.size(), customerIdStr);
        return items.stream()
                .map(policyItemMapper::toDomain)
                .toList();
    }
}
