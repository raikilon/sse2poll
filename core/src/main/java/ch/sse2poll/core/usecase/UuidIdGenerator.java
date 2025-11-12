package ch.sse2poll.core.usecase;

import java.util.UUID;

public final class UuidIdGenerator implements IdGenerator {
    @Override
    public String newId() {
        return UUID.randomUUID().toString();
    }
}

