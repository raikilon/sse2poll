package ch.sse2poll.core.framework.cache;

import ch.sse2poll.core.entities.model.Envelope;
import ch.sse2poll.core.entities.model.Pending;
import ch.sse2poll.core.entities.model.Ready;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CaffeineCacheClientTest {

    @Test
    void givenPendingEntry_whenReadBeforeExpiry_thenReturnsPending() {
        Context ctx = Context.fixedClock(Instant.parse("2024-01-01T00:00:00Z"));

        ctx.client.writePending("key", Duration.ofSeconds(5));

        Optional<Envelope> value = ctx.client.read("key", Object.class);
        assertTrue(value.isPresent());
        assertTrue(value.get() instanceof Pending);

        ctx.ticker.advanceSeconds(6);
        assertTrue(ctx.client.read("key", Object.class).isEmpty());
    }

    @Test
    void givenReadyEntry_whenRead_thenContainsPayloadAndTimestamp() {
        Instant completedAt = Instant.parse("2024-02-01T10:15:30Z");
        Context ctx = Context.fixedClock(completedAt);

        ctx.client.writeReady("job", "PAYLOAD", Duration.ofSeconds(10));

        Envelope envelope = ctx.client.read("job", Object.class).orElseThrow();
        assertTrue(envelope instanceof Ready<?>);
        Ready<?> ready = (Ready<?>) envelope;
        assertEquals("PAYLOAD", ready.payload());
        assertEquals(completedAt.toEpochMilli(), ready.ts());
    }

    @Test
    void givenEntry_whenDeleted_thenCannotBeRead() {
        Context ctx = Context.fixedClock(Instant.parse("2024-03-01T00:00:00Z"));

        ctx.client.writePending("job", Duration.ofSeconds(30));
        ctx.client.delete("job");

        assertTrue(ctx.client.read("job", Object.class).isEmpty());
    }

    private static final class Context {
        final CaffeineCacheClient client;
        final TestTicker ticker;

        Context(CaffeineCacheClient client, TestTicker ticker) {
            this.client = client;
            this.ticker = ticker;
        }

        static Context fixedClock(Instant instant) {
            TestTicker ticker = new TestTicker();
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            Cache<String, CaffeineCacheClient.StoredEnvelope> cache = Caffeine.newBuilder()
                    .ticker(ticker)
                    .expireAfter(new CaffeineCacheClient.StoredEnvelopeExpiry())
                    .build();
            return new Context(new CaffeineCacheClient(cache, clock, ticker), ticker);
        }
    }

    private static final class TestTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advanceSeconds(long seconds) {
            nanos.addAndGet(TimeUnit.SECONDS.toNanos(seconds));
        }
    }
}
