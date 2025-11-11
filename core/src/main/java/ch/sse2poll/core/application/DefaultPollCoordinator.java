package ch.sse2poll.core.application;

import ch.sse2poll.core.domain.model.Envelope;
import ch.sse2poll.core.domain.model.Ready;
import ch.sse2poll.core.domain.service.IdGenerator;
import ch.sse2poll.core.framework.annotation.PolledGet;
import ch.sse2poll.core.framework.config.Sse2PollProperties;
import ch.sse2poll.core.interfaceadapters.gateways.CacheClient;
import ch.sse2poll.core.interfaceadapters.presenters.Responses;
import ch.sse2poll.core.interfaceadapters.web.RequestContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * Default implementation coordinating kickoff and polling logic.
 */
public class DefaultPollCoordinator implements PollCoordinator {
    private final CacheClient cache;
    private final ExecutorService vt;
    private final Sse2PollProperties props;

    public DefaultPollCoordinator(CacheClient cache, ExecutorService vt, Sse2PollProperties props) {
        this.cache = cache;
        this.vt = vt;
        this.props = props;
    }

    @Override
    public Object handle(String ns, PolledGet ann, java.util.function.Supplier<Object> compute, RequestContext ctx) {
        int waitMs = clamp(ctx.queryInt("waitMs", props.getWaitMsDefault()), 0, props.getMaxWaitMs());
        String job = ctx.query("job");

        if (job != null && !job.isBlank()) {
            String key = key(ns, job);
            return pollExisting(key, ann, waitMs);
        }

        // kickoff without job: try compute within wait window
        String jobId = IdGenerator.ulid();
        String key = key(ns, jobId);

        cache.writePending(key, java.time.Duration.ofSeconds(ann.pendingTtl()));
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(compute, vt)
            .whenComplete((body, err) -> {
                if (err == null) cache.writeReady(key, body, java.time.Duration.ofSeconds(ann.ttl()));
            });

        try {
            Object body = future.get(waitMs, TimeUnit.MILLISECONDS);
            return Responses.ok(body); // finished fast -> payload only, no jobId
        } catch (Exception e) {
            return Responses.accepted(Map.of("jobId", jobId, "status", "pending"));
        }
    }

    private Object pollExisting(String key, PolledGet ann, int waitMs) {
        Optional<Envelope> env = cache.read(key, Object.class);
        if (env.isPresent() && env.get() instanceof Ready<?> r) return Responses.ok(r.payload());
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMs);
        while (System.nanoTime() < end) {
            Optional<Envelope> e2 = cache.read(key, Object.class);
            if (e2.isPresent() && e2.get() instanceof Ready<?> rr) return Responses.ok(rr.payload());
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(150));
        }
        return Responses.timeout(props.getStatusOnTimeout()); // 202 or 204
    }

    private static int clamp(int v, int lo, int hi){ return Math.max(lo, Math.min(hi, v)); }
    private String key(String ns, String job){ return props.getCache().getPrefix() + ":" + ns + ":" + job; }
}

