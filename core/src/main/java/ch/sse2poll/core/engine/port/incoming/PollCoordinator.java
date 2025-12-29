package ch.sse2poll.core.engine.port.incoming;

import java.util.function.Supplier;


public interface PollCoordinator {

    <T> T handle(String namespace,
                 Supplier<T> compute,
                 Class<T> responseType,
                 RequestContextView requestContext);

    interface RequestContextView {
        String clientJobId();
        long waitMs();
    }
}
