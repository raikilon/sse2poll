package ch.sse2poll.core.entities.model;

/**
 * Represents a still running job so pollers know to retry.
 *
 */
public record Pending() implements Envelope {
}

