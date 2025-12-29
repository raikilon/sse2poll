package ch.sse2poll.core.engine;

import ch.sse2poll.core.engine.port.incoming.PollCoordinator;
import ch.sse2poll.core.engine.port.outgoing.CacheClient;
import ch.sse2poll.core.engine.support.interfaces.AsyncRunner;
import ch.sse2poll.core.engine.support.interfaces.IdGenerator;
import ch.sse2poll.core.engine.support.interfaces.KeyFactory;
import ch.sse2poll.core.engine.support.interfaces.ReadyAwaiter;
import ch.sse2poll.core.engine.exception.PendingJobException;
import ch.sse2poll.core.engine.exception.UnknownJobException;
import ch.sse2poll.core.entities.model.Envelope;
import ch.sse2poll.core.entities.model.Pending;
import ch.sse2poll.core.entities.model.Ready;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public class CacheBackedPollCoordinator implements PollCoordinator {

    private final CacheClient cacheClient;
    private final IdGenerator idGenerator;
    private final KeyFactory keyFactory;
    private final ReadyAwaiter readyAwaiter;
    private final AsyncRunner asyncRunner;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public CacheBackedPollCoordinator(CacheClient cacheClient,
            IdGenerator idGenerator,
            KeyFactory keyFactory,
            ReadyAwaiter readyAwaiter,
            AsyncRunner asyncRunner) {
        this.cacheClient = cacheClient;
        this.idGenerator = idGenerator;
        this.keyFactory = keyFactory;
        this.readyAwaiter = readyAwaiter;
        this.asyncRunner = asyncRunner;
    }

    @Override
    public Object handle(
            String namespace,
            Supplier<?> compute,
            Class<?> responseType,
            RequestContextView requestContext) {
        String clientJobId = requestContext.clientJobId();
        long waitMs = requestContext.waitMs();

        if (clientJobId != null && !clientJobId.isBlank()) {
            return handlePoll(namespace, clientJobId, waitMs, responseType);
        }
        return handleKickoff(namespace, waitMs, compute, responseType);
    }

    private Object handlePoll(String namespace, String jobId, long waitMs, Class<?> responseType) {
        String key = keyFactory.build(namespace, jobId);

        return returnReadyOrPending(key, jobId, waitMs, responseType);
    }

    private Object handleKickoff(String namespace, long waitMs, Supplier<?> compute, Class<?> responseType) {
        String jobId = idGenerator.newId();
        String key = keyFactory.build(namespace, jobId);

        cacheClient.writePending(key, jobId, CACHE_TTL);

        asyncRunner.run(compute, payload -> cacheClient.writeReady(key, payload, CACHE_TTL));

        return returnReadyOrPending(key, jobId, waitMs, responseType);
    }

    private Optional<Ready<?>> waitForReady(String key, long waitMs, Class<?> responseType) {
        return readyAwaiter.waitReady(waitMs, () -> {
            Optional<Envelope> again = cacheClient.read(key, Object.class);
            if (again.isPresent() && again.get() instanceof Ready<?> r) {
                Ready<?> cast = castReady(r, responseType);
                return Optional.of(cast);
            }
            return Optional.empty();
        }).map(r -> (Ready<?>) r);
    }

    private Object returnReadyOrPending(String key, String jobId, long waitMs, Class<?> responseType) {
        Optional<Envelope> cached = cacheClient.read(key, Object.class);
        if (cached.isEmpty()) {
            throw new UnknownJobException(jobId);
        }

        if (waitMs > 0) {
            Optional<Ready<?>> ready = waitForReady(key, waitMs, responseType);
            if (ready.isPresent()) {
                return consumeReady(key, ready.get());
            }
        }

        Envelope envelope = cached.get();
        if (envelope instanceof Ready<?> ready) {
            return consumeReady(key, castReady(ready, responseType));
        }
        if (envelope instanceof Pending pending) {
            throw new PendingJobException(pending.jobId());
        }

        throw new IllegalStateException("Unsupported envelope type: " + envelope.getClass().getName());
    }

    private Object consumeReady(String key, Ready<?> ready) {
        cacheClient.delete(key);
        return ready.payload();
    }

    private Ready<?> castReady(Ready<?> ready, Class<?> responseType) {
        Object payload = ready.payload();
        if (!responseType.isInstance(payload)) {
            throw new ClassCastException("Cached payload of type "
                    + (payload == null ? "null" : payload.getClass().getName())
                    + " does not match expected " + responseType.getName());
        }
        return new Ready<>(responseType.cast(payload));
    }

}
