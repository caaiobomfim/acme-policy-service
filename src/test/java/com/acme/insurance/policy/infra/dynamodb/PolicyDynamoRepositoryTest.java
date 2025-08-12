package com.acme.insurance.policy.infra.dynamodb;

import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.infra.config.AppProps;
import com.acme.insurance.policy.infra.dynamodb.mapper.PolicyItemMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyDynamoRepositoryTest {

    private static class Ctx {
        final PolicyDynamoRepository repo;
        final DynamoDbTable<PolicyItem> table;
        final PolicyItemMapper mapper;
        final String gsiName;
        Ctx(PolicyDynamoRepository repo, DynamoDbTable<PolicyItem> table, PolicyItemMapper mapper, String gsiName) {
            this.repo = repo; this.table = table; this.mapper = mapper; this.gsiName = gsiName;
        }
    }

    private Ctx newRepoWithWiring() {

        DynamoDbClient ddb = mock(DynamoDbClient.class);
        var mapper = mock(PolicyItemMapper.class);
        var enhancedBuilder = mock(DynamoDbEnhancedClient.Builder.class, RETURNS_SELF);
        var enhanced = mock(DynamoDbEnhancedClient.class);
        @SuppressWarnings("unchecked")
        DynamoDbTable<PolicyItem> table = mock(DynamoDbTable.class);

        AppProps props = mock(AppProps.class, RETURNS_DEEP_STUBS);
        when(props.dynamodb().table()).thenReturn("policies-table-test");
        String gsiName = "gsi-customer-test";
        when(props.dynamodb().indexes().customer()).thenReturn(gsiName);

        try (MockedStatic<DynamoDbEnhancedClient> enh = mockStatic(DynamoDbEnhancedClient.class)) {
            enh.when(DynamoDbEnhancedClient::builder).thenReturn(enhancedBuilder);
            when(enhancedBuilder.dynamoDbClient(ddb)).thenReturn(enhancedBuilder);
            when(enhancedBuilder.build()).thenReturn(enhanced);

            when(enhanced.table(eq("policies-table-test"), any(TableSchema.class))).thenReturn(table);

            PolicyDynamoRepository repo = new PolicyDynamoRepository(ddb, props, mapper);
            return new Ctx(repo, table, mapper, gsiName);
        }
    }

    @Test
    @DisplayName("save() mapeia Policy→Item e faz putItem na tabela")
    void save_putsItem() {
        var ctx = newRepoWithWiring();

        Policy policy = mock(Policy.class);
        var item = new PolicyItem();
        item.setPolicyId(UUID.randomUUID().toString());
        item.setCustomerId(UUID.randomUUID().toString());

        when(ctx.mapper.toItem(policy)).thenReturn(item);

        ctx.repo.save(policy);

        verify(ctx.mapper).toItem(policy);
        verify(ctx.table).putItem(item);
        verifyNoMoreInteractions(ctx.table);
    }

    @Test
    @DisplayName("findById() quando encontra: retorna Optional com Policy mapeada")
    void findById_found() {
        var ctx = newRepoWithWiring();

        UUID id = UUID.randomUUID();
        var stored = new PolicyItem();
        stored.setPolicyId(id.toString());

        when(ctx.table.getItem(any(PolicyItem.class))).thenReturn(stored);

        Policy domain = mock(Policy.class);
        when(ctx.mapper.toDomain(stored)).thenReturn(domain);

        Optional<Policy> out = ctx.repo.findById(id);

        assertThat(out).isPresent().containsSame(domain);

        ArgumentCaptor<PolicyItem> cap = ArgumentCaptor.forClass(PolicyItem.class);
        verify(ctx.table).getItem(cap.capture());
        assertThat(cap.getValue().getPolicyId()).isEqualTo(id.toString());

        verify(ctx.mapper).toDomain(stored);
    }

    @Test
    @DisplayName("findById() quando não encontra: retorna Optional.empty() e não mapeia")
    void findById_notFound() {
        var ctx = newRepoWithWiring();

        when(ctx.table.getItem(any(PolicyItem.class))).thenReturn(null);

        Optional<Policy> out = ctx.repo.findById(UUID.randomUUID());

        assertThat(out).isEmpty();
        verify(ctx.table).getItem(any(PolicyItem.class));
        verify(ctx.mapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("findByCustomerId() consulta pelo GSI e mapeia todos os itens das páginas")
    void findByCustomerId_usesGsi_andMapsPages() {
        var ctx = newRepoWithWiring();

        UUID customerId = UUID.randomUUID();

        @SuppressWarnings("unchecked")
        DynamoDbIndex<PolicyItem> index = mock(DynamoDbIndex.class);
        when(ctx.table.index(ctx.gsiName)).thenReturn(index);

        @SuppressWarnings("unchecked")
        PageIterable<PolicyItem> pages = mock(PageIterable.class);

        when(index.query(any(Consumer.class))).thenReturn(pages);

        @SuppressWarnings("unchecked")
        Page<PolicyItem> page1 = mock(Page.class);
        @SuppressWarnings("unchecked")
        Page<PolicyItem> page2 = mock(Page.class);

        var i1 = new PolicyItem(); i1.setPolicyId("p1"); i1.setCustomerId(customerId.toString());
        var i2 = new PolicyItem(); i2.setPolicyId("p2"); i2.setCustomerId(customerId.toString());
        when(page1.items()).thenReturn(List.of(i1));
        when(page2.items()).thenReturn(List.of(i2));

        when(pages.stream()).thenReturn(Stream.of(page1, page2));

        Policy d1 = mock(Policy.class);
        Policy d2 = mock(Policy.class);
        when(ctx.mapper.toDomain(i1)).thenReturn(d1);
        when(ctx.mapper.toDomain(i2)).thenReturn(d2);

        List<Policy> out = ctx.repo.findByCustomerId(customerId);

        assertThat(out).containsExactly(d1, d2);

        verify(ctx.table).index(ctx.gsiName);
        verify(index).query(any(Consumer.class));
        verify(pages).stream();
        verify(page1).items();
        verify(page2).items();
        verify(ctx.mapper).toDomain(i1);
        verify(ctx.mapper).toDomain(i2);
    }
}
