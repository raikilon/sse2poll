package ch.sse2poll.core.interfaceadapters.gateways;

/**
 * Simple serializer abstraction.
 */
public interface Serializer {
    String serialize(Object value);
    <T> T deserialize(String json, Class<T> type);
}

