package ch.sse2poll.core.interfaceadapters.presenters;

import ch.sse2poll.core.domain.model.Envelope;
import ch.sse2poll.core.domain.model.Ready;

/**
 * Presenter that maps envelopes to response bodies.
 */
public final class PollPresenter {
    private PollPresenter() {}

    public static Object toResponse(Envelope env) {
        if (env instanceof Ready<?> r) return Responses.ok(r.payload());
        return Responses.accepted(java.util.Map.of("status", "pending"));
    }
}

