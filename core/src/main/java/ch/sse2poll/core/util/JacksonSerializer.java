package ch.sse2poll.core.util;

import ch.sse2poll.core.interfaceadapters.gateways.Serializer;

/**
 * Placeholder serializer that does not depend on Jackson to keep the module lean.
 * Implement with your JSON library of choice in consuming applications.
 */
public class JacksonSerializer implements Serializer {
    @Override
    public String serialize(Object value) {
        return String.valueOf(value);
    }

    @Override
    public <T> T deserialize(String json, Class<T> type) {
        return null; // not implemented
    }
}

