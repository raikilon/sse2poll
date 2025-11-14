package ch.sse2poll.core.application;

public final class DefaultKeyFactory implements KeyFactory {
    @Override
    public String build(String namespace, String jobId) {
        return namespace + ":" + jobId;
    }
}

