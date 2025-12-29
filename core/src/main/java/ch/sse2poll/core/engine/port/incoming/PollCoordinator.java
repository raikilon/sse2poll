package ch.sse2poll.core.engine.port.incoming;

import java.util.function.Supplier;


public interface PollCoordinator {

    Object handle(String namespace,
                  Supplier<?> compute,
                  Class<?> responseType,
                  RequestContextView requestContext);

    interface RequestContextView {
        String clientJobId();
        long waitMs();
    }
}
