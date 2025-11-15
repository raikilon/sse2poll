package ch.sse2poll.core.engine.support.interfaces;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface AsyncRunner {
    void run(Supplier<Object> compute, Consumer<Object> onSuccess);
}

