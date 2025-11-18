package ch.sse2poll.core.engine.support.implementation;

import java.util.UUID;

import ch.sse2poll.core.engine.support.interfaces.IdGenerator;

public final class UuidIdGenerator implements IdGenerator {
    @Override
    public String newId() {
        return UUID.randomUUID().toString();
    }
}

