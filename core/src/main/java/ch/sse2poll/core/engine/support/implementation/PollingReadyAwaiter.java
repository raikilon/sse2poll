package ch.sse2poll.core.engine.support.implementation;

import ch.sse2poll.core.engine.support.interfaces.ReadyAwaiter;
import ch.sse2poll.core.entities.model.Ready;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class PollingReadyAwaiter implements ReadyAwaiter {
    @Override
    public <T> Optional<Ready<T>> waitReady(long waitMs, Supplier<Optional<Ready<T>>> tryConsumeReady) {
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMs);
        while (System.nanoTime() < end) {
            Optional<Ready<T>> res = tryConsumeReady.get();
            if (res.isPresent()) {
                return res;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return Optional.empty();
    }
}
