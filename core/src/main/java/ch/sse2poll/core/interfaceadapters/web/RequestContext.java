package ch.sse2poll.core.interfaceadapters.web;

import java.util.Collections;
import java.util.Map;

/**
 * Minimal request context abstraction for query access.
 */
public final class RequestContext {
    private static final ThreadLocal<RequestContext> CURRENT = ThreadLocal.withInitial(RequestContext::new);

    private final Map<String, String> query;

    public RequestContext() {
        this(Collections.emptyMap());
    }

    public RequestContext(Map<String, String> query) {
        this.query = query == null ? Collections.emptyMap() : Map.copyOf(query);
    }

    public static RequestContext current() {
        return CURRENT.get();
    }

    public static void setCurrent(RequestContext ctx) {
        CURRENT.set(ctx);
    }

    public String query(String name) {
        return query.get(name);
    }

    public int queryInt(String name, int defaultValue) {
        String v = query(name);
        if (v == null || v.isBlank()) return defaultValue;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return defaultValue; }
    }
}

