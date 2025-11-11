package ch.sse2poll.core.domain.model;

/**
 * Ready envelope carrying a payload and timestamp.
 */
public record Ready<T>(long ts, T payload) implements Envelope {}

