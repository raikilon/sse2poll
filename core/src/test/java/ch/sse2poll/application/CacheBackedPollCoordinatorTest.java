import org.junit.jupiter.api.Test;

import ch.sse2poll.core.application.AsyncRunner;
import ch.sse2poll.core.application.CacheBackedPollCoordinator;
import ch.sse2poll.core.application.IdGenerator;
import ch.sse2poll.core.application.KeyFactory;
import ch.sse2poll.core.application.ReadyAwaiter;
import ch.sse2poll.core.application.port.incoming.PollCoordinator;
import ch.sse2poll.core.application.port.outgoing.CacheClient;
import ch.sse2poll.core.entities.model.Pending;
import ch.sse2poll.core.entities.model.Ready;
import ch.sse2poll.core.entities.model.Envelope;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class CacheBackedPollCoordinatorTest {

    @Test
    void givenNewJobAndAwaitEnabled_WhenComputeCompletesQuickly_ThenReturnsReadyAndDeletesKey() {
        Context ctx = Context.immediateDefaults("jid-1");
        Object res = ctx.coordinator().handle("ns", () -> "OK", Context.rc(null, 200));

        assertTrue(res instanceof Ready<?>);
        assertEquals("OK", ((Ready<?>) res).payload());
        assertEquals("ns:jid-1", ctx.cache.lastDeletedKey);
        assertTrue(ctx.cache.read("ns:jid-1", Object.class).isEmpty());
    }

    @Test
    void givenNewJobAndNoWait_WhenComputeDeferred_ThenReturnsPendingAndKeepsKey() {
        Context.InMemoryCache cache = new Context.InMemoryCache();
        Context ctx = new Context(
                cache,
                new Context.FixedIdGenerator("jid-2"),
                new Context.SimpleKeyFactory(),
                new Context.OneShotReadyAwaiter(),
                new Context.DeferringAsyncRunner());

        Object res = ctx.coordinator().handle("ns", () -> "LATE", Context.rc(null, 0));

        assertTrue(res instanceof Pending);
        Optional<Envelope> v = cache.read("ns:jid-2", Object.class);
        assertTrue(v.isPresent());
        assertTrue(v.get() instanceof Pending);
        assertNull(cache.lastDeletedKey);
    }

    @Test
    void givenMissingKey_WhenPoll_ThenThrowsUnknownJobId() {
        Context ctx = Context.immediateDefaults("unused");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ctx.coordinator().handle("ns", () -> "IGNORED", Context.rc("missing", 0)));
        assertTrue(ex.getMessage().startsWith("Unknown job id"));
    }

    @Test
    void givenReadyInCache_WhenPoll_ThenReturnsReadyAndDeletesKey() {
        Context.InMemoryCache cache = new Context.InMemoryCache();
        Context.SimpleKeyFactory keys = new Context.SimpleKeyFactory();
        String key = keys.build("ns", "jid-3");
        cache.writeReady(key, "PAY", Duration.ofMinutes(5));

        Context ctx = new Context(
                cache,
                new Context.FixedIdGenerator("unused"),
                keys,
                new Context.OneShotReadyAwaiter(),
                new Context.ImmediateAsyncRunner());

        Object res = ctx.coordinator().handle("ns", () -> "IGNORED", Context.rc("jid-3", 0));

        assertTrue(res instanceof Ready<?>);
        assertEquals("PAY", ((Ready<?>) res).payload());
        assertEquals(key, cache.lastDeletedKey);
        assertTrue(cache.read(key, Object.class).isEmpty());
    }

    static final class Context {
        final InMemoryCache cache;
        final IdGenerator idGen;
        final KeyFactory keys;
        final ReadyAwaiter awaiter;
        final AsyncRunner async;

        Context(InMemoryCache cache, IdGenerator idGen, KeyFactory keys, ReadyAwaiter awaiter, AsyncRunner async) {
            this.cache = cache;
            this.idGen = idGen;
            this.keys = keys;
            this.awaiter = awaiter;
            this.async = async;
        }

        static Context immediateDefaults(String fixedId) {
            return new Context(new InMemoryCache(), new FixedIdGenerator(fixedId), new SimpleKeyFactory(),
                    new OneShotReadyAwaiter(), new ImmediateAsyncRunner());
        }

        CacheBackedPollCoordinator coordinator() {
            return new CacheBackedPollCoordinator(cache, idGen, keys, awaiter, async);
        }

        static PollCoordinator.RequestContextView rc(String jobId, long waitMs) {
            return new PollCoordinator.RequestContextView() {
                @Override
                public String clientJobId() {
                    return jobId;
                }

                @Override
                public long waitMs() {
                    return waitMs;
                }
            };
        }

        static final class InMemoryCache implements CacheClient {
            final Map<String, Envelope> store = new ConcurrentHashMap<>();
            volatile String lastDeletedKey;

            @Override
            public Optional<Envelope> read(String key, Class<?> bodyType) {
                return Optional.ofNullable(store.get(key));
            }

            @Override
            public void writePending(String key, Duration ttl) {
                store.put(key, new Pending());
            }

            @Override
            public void writeReady(String key, Object payload, Duration ttl) {
                store.put(key, new Ready<>(System.currentTimeMillis(), payload));
            }

            @Override
            public void delete(String key) {
                store.remove(key);
                lastDeletedKey = key;
            }
        }

        static final class ImmediateAsyncRunner implements AsyncRunner {
            @Override
            public void run(Supplier<Object> compute, Consumer<Object> onSuccess) {
                onSuccess.accept(compute.get());
            }
        }

        static final class DeferringAsyncRunner implements AsyncRunner {
            @Override
            public void run(Supplier<Object> compute, Consumer<Object> onSuccess) {
            }
        }

        static final class OneShotReadyAwaiter implements ReadyAwaiter {
            @Override
            public Optional<Ready<?>> waitReady(long waitMs, Supplier<Optional<Ready<?>>> tryConsumeReady) {
                return tryConsumeReady.get();
            }
        }

        static final class FixedIdGenerator implements IdGenerator {
            private final String id;

            FixedIdGenerator(String id) {
                this.id = id;
            }

            @Override
            public String newId() {
                return id;
            }
        }

        static final class SimpleKeyFactory implements KeyFactory {
            @Override
            public String build(String namespace, String jobId) {
                return namespace + ":" + jobId;
            }
        }
    }
}
