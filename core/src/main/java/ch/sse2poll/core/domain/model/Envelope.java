package ch.sse2poll.core.domain.model;

/**
 * Envelope for pollable responses.
 */
public sealed interface Envelope permits Ready, Pending {}

