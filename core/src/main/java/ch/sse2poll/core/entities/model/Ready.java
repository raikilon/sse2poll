package ch.sse2poll.core.entities.model;

public record Ready<T>(T payload) implements Envelope {
}
