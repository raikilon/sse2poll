package ch.sse2poll.core.application.commands;

/**
 * Command object representing a polling request.
 */
public record PollJobCommand(String namespace, String jobId, int waitMs) {}

