package ch.sse2poll.core.interfaceadapters.gateways;

import ch.sse2poll.core.domain.model.Envelope;
import ch.sse2poll.core.domain.model.Pending;
import ch.sse2poll.core.domain.model.Ready;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory implementation emulating Caffeine behavior.
 * No expiration logic is applied here; intended for tests/demo only.
 */
public class CaffeineCacheClient implements CacheClient {
    private final Map<String, Envelope> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Envelope> read(String key, Class<?> bodyType) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public void writePending(String key, Duration pendingTtl) {
        store.put(key, new Pending(System.currentTimeMillis()));
    }

    @Override
    public void writeReady(String key, Object payload, Duration valueTtl) {
        store.put(key, new Ready<>(System.currentTimeMillis(), payload));
    }
}

