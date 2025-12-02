package ch.sse2poll.core.framework.cache;

import ch.sse2poll.core.entities.model.Envelope;
import ch.sse2poll.core.entities.model.Pending;
import ch.sse2poll.core.entities.model.Ready;
import ch.sse2poll.core.engine.port.outgoing.CacheClient;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * CacheClient implementation backed by Caffeine.
 */
public final class CaffeineCacheClient implements CacheClient {

    private final Cache<String, StoredEnvelope> cache;
    private final Ticker ticker;

    public CaffeineCacheClient(long maximumSize) {
        Ticker ticker = Ticker.systemTicker();
        this(buildCache(maximumSize, ticker), ticker);
    }

    public CaffeineCacheClient(Cache<String, StoredEnvelope> cache, Ticker ticker) {
        this.cache = Objects.requireNonNull(cache, "cache");
        this.ticker = Objects.requireNonNull(ticker, "ticker");
    }

    private static Cache<String, StoredEnvelope> buildCache(long maximumSize, Ticker ticker) {
        long safeSize = Math.max(1L, maximumSize);
        return Caffeine.newBuilder()
                .maximumSize(safeSize)
                .ticker(ticker)
                .expireAfter(new StoredEnvelopeExpiry())
                .build();
    }

    @Override
    public Optional<Envelope> read(String key, Class<?> bodyType) {
        StoredEnvelope stored = cache.getIfPresent(key);
        return stored == null ? Optional.empty() : Optional.of(stored.envelope());
    }

    @Override
    public void writePending(String key, String jobId, Duration ttl) {
        cache.put(key, StoredEnvelope.pending(jobId, ticker.read(), ttl));
    }

    @Override
    public <T> void writeReady(String key, T payload, Duration ttl) {
        cache.put(key, StoredEnvelope.ready(payload, ticker.read(), ttl));
    }

    @Override
    public void delete(String key) {
        cache.invalidate(key);
    }

    public static final class StoredEnvelope {
        private final Envelope envelope;
        private final long expiresAtNanos;

        private StoredEnvelope(Envelope envelope, long expiresAtNanos) {
            this.envelope = Objects.requireNonNull(envelope, "envelope");
            this.expiresAtNanos = expiresAtNanos;
        }

        public Envelope envelope() {
            return envelope;
        }

        long expiresInNanos(long nowNanos) {
            long remaining = expiresAtNanos - nowNanos;
            return remaining <= 0 ? 0 : remaining;
        }

        static StoredEnvelope pending(String jobId, long nowNanos, Duration ttl) {
            return new StoredEnvelope(new Pending(jobId), expiresAt(nowNanos, ttl));
        }

        static StoredEnvelope ready(Object payload, long nowNanos, Duration ttl) {
            return new StoredEnvelope(new Ready<>(payload), expiresAt(nowNanos, ttl));
        }

        private static long expiresAt(long nowNanos, Duration ttl) {
            long ttlNanos = normalize(ttl);
            if (ttlNanos == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long expires = nowNanos + ttlNanos;
            if (expires < 0) {
                return Long.MAX_VALUE;
            }
            return expires;
        }

        private static long normalize(Duration ttl) {
            if (ttl == null) {
                return TimeUnit.SECONDS.toNanos(1);
            }
            long nanos;
            try {
                nanos = ttl.toNanos();
            } catch (ArithmeticException ex) {
                return Long.MAX_VALUE;
            }
            return nanos <= 0 ? 1L : nanos;
        }
    }

    public static final class StoredEnvelopeExpiry implements Expiry<String, StoredEnvelope> {
        @Override
        public long expireAfterCreate(String key, StoredEnvelope value, long currentTime) {
            return value.expiresInNanos(currentTime);
        }

        @Override
        public long expireAfterUpdate(String key, StoredEnvelope value, long currentTime, long currentDuration) {
            return value.expiresInNanos(currentTime);
        }

        @Override
        public long expireAfterRead(String key, StoredEnvelope value, long currentTime, long currentDuration) {
            return value.expiresInNanos(currentTime);
        }
    }
}
