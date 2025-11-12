package ch.sse2poll.core.usecase;

public interface KeyFactory {
    String build(String namespace, String jobId);
}

