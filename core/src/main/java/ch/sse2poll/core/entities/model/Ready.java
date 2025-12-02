package ch.sse2poll.core.entities.model;

/**
 * Represents a finished job along with the computed payload.
 *
 * @param payload controller response to return to clients
 */
public record Ready<T>(T payload) implements Envelope {
}
