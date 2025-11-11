package ch.sse2poll.core.domain.service;

/**
 * Builds cache keys in the format: prefix:namespace:jobId
 */
public final class JobKeyFactory {
    private JobKeyFactory() {}

    public static String build(String prefix, String namespace, String jobId) {
        return prefix + ":" + namespace + ":" + jobId;
    }
}

