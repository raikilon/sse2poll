package ch.sse2poll.core.interfaceadapters.presenters;

import java.util.Map;

/**
 * Minimal presenter helpers mapping to HTTP-like responses.
 * This is intentionally simple to avoid extra dependencies.
 */
public final class Responses {
    private Responses() {}

    public static Object ok(Object body) {
        return body;
    }

    public static Map<String, Object> accepted(Map<String, Object> body) {
        return body;
    }

    public static Map<String, Object> timeout(int status) {
        return Map.of("status", status);
    }
}

