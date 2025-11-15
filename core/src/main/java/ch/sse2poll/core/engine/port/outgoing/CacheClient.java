package ch.sse2poll.core.engine.port.outgoing;

import ch.sse2poll.core.entities.model.Envelope;

import java.time.Duration;
import java.util.Optional;

/**
 * Abstraction over the cache (Caffeine, Redis, etc.) used to store poll envelopes.
 */
public interface CacheClient {

    Optional<Envelope> read(String key, Class<?> bodyType);

    void writePending(String key, Duration ttl);

    void writeReady(String key, Object payload, Duration ttl);

    void delete(String key);

}
