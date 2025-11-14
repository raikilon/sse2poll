package ch.sse2poll.core.application.port.incoming;

import java.util.function.Supplier;

/**
 * Use-case boundary for orchestrating polled computations.
 */
public interface PollCoordinator {

    Object handle(String namespace,
                  Supplier<Object> compute,
                  RequestContextView requestContext);

    /**
     * Minimal contract so we are not tied to framework annotations here.
     */
    interface AnnotationView {
        long waitMs();
    }

    /**
     * Represents HTTP request metadata needed by the coordinator (headers, query params, etc.).
     */
    interface RequestContextView {
        String clientJobId();
        long waitMs();
    }
}

