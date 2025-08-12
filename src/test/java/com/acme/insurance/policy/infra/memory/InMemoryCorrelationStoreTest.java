package com.acme.insurance.policy.infra.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCorrelationStoreTest {

    private final InMemoryCorrelationStore store = new InMemoryCorrelationStore();

    @Test
    @DisplayName("Inicialmente bothDone=false e após uma única marcação continua false")
    void bothDone_initial_and_single_mark() {
        UUID id = UUID.randomUUID();
        assertThat(store.bothDone(id)).isFalse();

        store.markPayment(id, Instant.now());
        assertThat(store.bothDone(id)).isFalse();

        UUID id2 = UUID.randomUUID();
        store.markSubscription(id2, Instant.now());
        assertThat(store.bothDone(id2)).isFalse();
    }

    @Test
    @DisplayName("Marcações acumulam independente da ordem e bothDone=true")
    void marks_accumulate_any_order() {
        Instant now = Instant.now();

        UUID id1 = UUID.randomUUID();
        store.markPayment(id1, now);
        store.markSubscription(id1, now);
        assertThat(store.bothDone(id1)).isTrue();

        UUID id2 = UUID.randomUUID();
        store.markSubscription(id2, now);
        store.markPayment(id2, now);
        assertThat(store.bothDone(id2)).isTrue();
    }

    @Test
    @DisplayName("clear remove a entrada")
    void clear_removes_entry() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        store.markPayment(id, now);
        store.markSubscription(id, now);
        assertThat(store.bothDone(id)).isTrue();

        store.clear(id);
        assertThat(store.bothDone(id)).isFalse();
    }

    @Test
    @DisplayName("evictExpired remove entradas antigas (TTL=30min)")
    void evictExpired_removes_old_entries() {
        UUID id = UUID.randomUUID();
        Instant old = Instant.now().minus(Duration.ofHours(1));

        store.markPayment(id, old);
        store.markSubscription(id, old);
        assertThat(store.bothDone(id)).isTrue();

        store.evictExpired();
        assertThat(store.bothDone(id)).isFalse();
    }

    @Test
    @DisplayName("evictExpired mantém entradas recentes")
    void evictExpired_keeps_recent_entries() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        store.markPayment(id, now);
        store.markSubscription(id, now);

        store.evictExpired();
        assertThat(store.bothDone(id)).isTrue();
    }
}