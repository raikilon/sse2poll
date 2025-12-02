package ch.sse2poll.core.engine.port.incoming;

import java.util.function.Supplier;

/**
 * Use-case boundary for orchestrating polled computations.
 */
public interface PollCoordinator {

    <T> T handle(String namespace,
                 Supplier<T> compute,
                 Class<T> responseType,
                 RequestContextView requestContext);

    /**
     * Represents HTTP request metadata needed by the coordinator (headers, query params, etc.).
     */
    interface RequestContextView {
        String clientJobId();
        long waitMs();
    }
}
