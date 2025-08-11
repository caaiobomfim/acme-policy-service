package com.acme.insurance.policy.infra.memory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryCorrelationStore {

    public enum Mark { PAYMENT_CONFIRMED, SUBSCRIPTION_AUTHORIZED }
    public record Entry(EnumSet<Mark> marks, Instant lastUpdate) {}

    private final ConcurrentHashMap<UUID, Entry> map = new ConcurrentHashMap<>();
    private final Duration ttl = Duration.ofMinutes(30);

    public void markPayment(UUID id, Instant when) {
        map.compute(id, (k, v) -> {
            var set = v == null ? EnumSet.noneOf(Mark.class) : EnumSet.copyOf(v.marks());
            set.add(Mark.PAYMENT_CONFIRMED);
            return new Entry(set, when);
        });
    }

    public void markSubscription(UUID id, Instant when) {
        map.compute(id, (k, v) -> {
            var set = v == null ? EnumSet.noneOf(Mark.class) : EnumSet.copyOf(v.marks());
            set.add(Mark.SUBSCRIPTION_AUTHORIZED);
            return new Entry(set, when);
        });
    }

    public boolean bothDone(UUID id) {
        var v = map.get(id);
        return v != null && v.marks().containsAll(EnumSet.of(Mark.PAYMENT_CONFIRMED, Mark.SUBSCRIPTION_AUTHORIZED));
    }

    public void clear(UUID id) { map.remove(id); }

    @Scheduled(fixedDelay = 300_000)
    void evictExpired() {
        var cutoff = Instant.now().minus(ttl);
        map.entrySet().removeIf(e -> e.getValue().lastUpdate().isBefore(cutoff));
    }
}
