package ch.sse2poll.core.usecase;

import ch.sse2poll.core.entities.model.Ready;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class DefaultReadyAwaiter implements ReadyAwaiter {
    @Override
    public Optional<Ready<?>> waitReady(long waitMs, Supplier<Optional<Ready<?>>> tryConsumeReady) {
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMs);
        while (System.nanoTime() < end) {
            Optional<Ready<?>> res = tryConsumeReady.get();
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
