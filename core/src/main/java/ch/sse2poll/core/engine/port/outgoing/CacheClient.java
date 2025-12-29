package ch.sse2poll.core.engine.port.outgoing;

import ch.sse2poll.core.entities.model.Envelope;

import java.time.Duration;
import java.util.Optional;

public interface CacheClient {

    Optional<Envelope> read(String key, Class<?> bodyType);

    void writePending(String key, String jobId, Duration ttl);

    <T> void writeReady(String key, T payload, Duration ttl);

    void delete(String key);

}
