package ch.sse2poll.core.engine.support.implementation;

import java.util.function.Consumer;
import java.util.function.Supplier;

import ch.sse2poll.core.engine.support.interfaces.AsyncRunner;


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

