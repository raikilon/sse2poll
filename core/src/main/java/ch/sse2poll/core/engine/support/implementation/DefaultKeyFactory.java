package ch.sse2poll.core.engine.support.implementation;

import ch.sse2poll.core.engine.support.interfaces.KeyFactory;

public final class DefaultKeyFactory implements KeyFactory {
    @Override
    public String build(String namespace, String jobId) {
        return namespace + ":" + jobId;
    }
}

