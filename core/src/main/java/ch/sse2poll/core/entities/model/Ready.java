package ch.sse2poll.core.entities.model;

/**
 * Represents a finished job along with the computed payload.
 *
 * @param ts      completion timestamp (epoch millis)
 * @param payload controller response to return to clients
 */
public record Ready<T>(long ts, T payload) implements Envelope {
}

