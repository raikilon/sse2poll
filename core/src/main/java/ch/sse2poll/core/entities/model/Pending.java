package ch.sse2poll.core.entities.model;

/**
 * Represents a still running job so pollers know to retry.
 *
 * @param jobId identifier clients must use when polling
 */
public record Pending(String jobId) implements Envelope {
}
