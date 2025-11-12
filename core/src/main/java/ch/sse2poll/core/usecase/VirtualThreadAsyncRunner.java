package ch.sse2poll.core.usecase;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class VirtualThreadAsyncRunner implements AsyncRunner {
    @Override
    public void run(Supplier<Object> compute, Consumer<Object> onSuccess) {
        Thread.ofVirtual().start(() -> {
            try {
                Object payload = compute.get();
                onSuccess.accept(payload);
            } catch (Throwable t) {
                // swallow errors; callers handle via cache TTL
            }
        });
    }
}

