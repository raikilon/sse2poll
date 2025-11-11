package ch.sse2poll.core.interfaceadapters.gateways;

import ch.sse2poll.core.domain.model.Envelope;

import java.time.Duration;
import java.util.Optional;

/**
 * Placeholder for a Redis-backed implementation. Not implemented to keep module dependency-free.
 */
public class RedisCacheClient implements CacheClient {
    @Override
    public Optional<Envelope> read(String key, Class<?> bodyType) {
        return Optional.empty();
    }

    @Override
    public void writePending(String key, Duration pendingTtl) {
        // no-op
    }

    @Override
    public void writeReady(String key, Object payload, Duration valueTtl) {
        // no-op
    }
}

