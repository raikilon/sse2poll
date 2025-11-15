package ch.sse2poll.core.engine.support.interfaces;

import ch.sse2poll.core.entities.model.Ready;

import java.util.Optional;
import java.util.function.Supplier;

public interface ReadyAwaiter {
    Optional<Ready<?>> waitReady(long waitMs, Supplier<Optional<Ready<?>>> tryConsumeReady);
}
