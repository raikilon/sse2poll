package ch.sse2poll.core.domain.model;

/**
 * Pending envelope carrying start timestamp.
 */
public record Pending(long startedAt) implements Envelope {}

