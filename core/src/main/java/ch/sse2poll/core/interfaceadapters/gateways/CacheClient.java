package ch.sse2poll.core.interfaceadapters.gateways;

import ch.sse2poll.core.domain.model.Envelope;

import java.time.Duration;
import java.util.Optional;

/**
 * Backend SPI for reading/writing poll envelopes.
 */
public interface CacheClient {
    Optional<Envelope> read(String key, Class<?> bodyType);
    void writePending(String key, Duration pendingTtl);
    void writeReady(String key, Object payload, Duration valueTtl);
    default void touch(String key, Duration ttl) {}
}

