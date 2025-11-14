package ch.sse2poll.core.application;

import ch.sse2poll.core.application.port.incoming.PollCoordinator;
import ch.sse2poll.core.application.port.outgoing.CacheClient;
import ch.sse2poll.core.entities.model.Envelope;
import ch.sse2poll.core.entities.model.Ready;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Poll coordinator that uses a CacheClient to persist job envelopes between
 * requests.
 */
public class CacheBackedPollCoordinator implements PollCoordinator {

    private final CacheClient cacheClient;
    private final IdGenerator idGenerator;
    private final KeyFactory keyFactory;
    private final ReadyAwaiter readyAwaiter;
    private final AsyncRunner asyncRunner;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public CacheBackedPollCoordinator(CacheClient cacheClient) {
        this(cacheClient,
                new UuidIdGenerator(),
                new DefaultKeyFactory(),
                new PollingReadyAwaiter(),
                new VirtualThreadAsyncRunner());
    }

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
            Supplier<Object> compute,
            RequestContextView requestContext) {
        String clientJobId = requestContext.clientJobId();
        long waitMs = requestContext.waitMs();

        if (clientJobId != null && !clientJobId.isBlank()) {
            return handlePoll(namespace, clientJobId, waitMs);
        }
        return handleKickoff(namespace, waitMs, compute);
    }

    private Object handlePoll(String namespace, String jobId, long waitMs) {
        String key = keyFactory.build(namespace, jobId);

        return returnReadyOrPending(key, waitMs);
    }

    private Object handleKickoff(String namespace, long waitMs, Supplier<Object> compute) {
        String jobId = idGenerator.newId();
        String key = keyFactory.build(namespace, jobId);

        cacheClient.writePending(key, CACHE_TTL);

        asyncRunner.run(compute, payload -> cacheClient.writeReady(key, payload, CACHE_TTL));

        return returnReadyOrPending(key, waitMs);
    }

    private Optional<Ready<?>> waitForReady(String key, long waitMs) {
        return readyAwaiter.waitReady(waitMs, () -> {
            Optional<Envelope> again = cacheClient.read(key, Object.class);
            if (again.isPresent() && again.get() instanceof Ready<?> r) {
                cacheClient.delete(key);
                return Optional.of((Ready<?>) r);
            }
            return Optional.empty();
        });
    }

    private Object returnReadyOrPending(String key, long waitMs) {
        Optional<Envelope> cached = cacheClient.read(key, Object.class);
        if (cached.isEmpty()) {
            throw new IllegalArgumentException("Unknown job id: ");
        }

        if (waitMs > 0) {
            Optional<Ready<?>> ready = waitForReady(key, waitMs);
            if (ready.isPresent()) {
                return ready.get();
            }
        }

        Envelope envelope = cached.get();
        if (envelope instanceof Ready<?>) {
            cacheClient.delete(key);
        }

        return envelope;
    }

}
