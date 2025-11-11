package ch.sse2poll.core.interfaceadapters.web;

import ch.sse2poll.core.application.PollCoordinator;
import ch.sse2poll.core.framework.annotation.PolledGet;

import java.lang.reflect.Method;

/**
 * Placeholder aspect-like adapter. Does not depend on Spring; you can wire it
 * with your AOP framework and delegate to {@link PollCoordinator}.
 */
public final class PolledGetAspect {
    private final PollCoordinator coordinator;

    public PolledGetAspect(PollCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Frameworks can call this method to emulate the around-advice behavior.
     */
    public Object around(Object target, Method method, PolledGet ann, Invoker invoker) {
        String namespace = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        var ctx = RequestContext.current();
        return coordinator.handle(namespace, ann, () -> invoker.invoke(), ctx);
    }

    @FunctionalInterface
    public interface Invoker { Object invoke(); }
}

