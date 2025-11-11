package ch.sse2poll.demo;

import ch.sse2poll.core.application.DefaultPollCoordinator;
import ch.sse2poll.core.framework.annotation.PolledGet;
import ch.sse2poll.core.framework.config.Sse2PollProperties;
import ch.sse2poll.core.interfaceadapters.gateways.CaffeineCacheClient;
import ch.sse2poll.core.interfaceadapters.web.RequestContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPollCoordinatorDemoTest {

    private static PolledGet ann(int ttl, int pendingTtl) {
        return new PolledGet() {
            @Override public int ttl() { return ttl; }
            @Override public int pendingTtl() { return pendingTtl; }
            @Override public Class<? extends Annotation> annotationType() { return PolledGet.class; }
        };
    }

    @Test
    void kickoff_times_out_then_poll_returns_ready() {
        var cache = new CaffeineCacheClient();
        ExecutorService vt = Executors.newCachedThreadPool();
        var props = new Sse2PollProperties();
        var coordinator = new DefaultPollCoordinator(cache, vt, props);

        String ns = "ReportsController#daily";
        var annotation = ann(300, 30);

        // Kickoff with small waitMs so we get a 202-like Map containing jobId
        Object kickoff = coordinator.handle(ns, annotation, () -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            return "PAYLOAD";
        }, new RequestContext(Map.of("waitMs", "100")));

        assertTrue(kickoff instanceof Map, "Expected accepted-like map");
        @SuppressWarnings("unchecked")
        Map<String, Object> accepted = (Map<String, Object>) kickoff;
        assertEquals("pending", accepted.get("status"));
        String jobId = (String) accepted.get("jobId");
        assertNotNull(jobId);

        // Poll with jobId and larger waitMs so we receive the ready payload
        Object polled = coordinator.handle(ns, annotation, () -> "SHOULD_NOT_BE_CALLED",
                new RequestContext(Map.of("job", jobId, "waitMs", "2000")));

        assertEquals("PAYLOAD", polled);
        vt.shutdownNow();
    }

    @Test
    void fast_path_returns_payload_immediately() {
        var cache = new CaffeineCacheClient();
        ExecutorService vt = Executors.newCachedThreadPool();
        var props = new Sse2PollProperties();
        var coordinator = new DefaultPollCoordinator(cache, vt, props);

        String ns = "ReportsController#daily";
        var annotation = ann(300, 30);

        Object result = coordinator.handle(ns, annotation, () -> "FAST", new RequestContext(Map.of("waitMs", "1000")));
        assertEquals("FAST", result);
        vt.shutdownNow();
    }
}

