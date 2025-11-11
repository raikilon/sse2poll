package ch.sse2poll.core.application;

import ch.sse2poll.core.framework.annotation.PolledGet;
import ch.sse2poll.core.interfaceadapters.web.RequestContext;

import java.util.function.Supplier;

/**
 * Use case boundary for handling kickoff and polling.
 */
public interface PollCoordinator {
    Object handle(String namespace, PolledGet ann, Supplier<Object> compute, RequestContext ctx);
}

