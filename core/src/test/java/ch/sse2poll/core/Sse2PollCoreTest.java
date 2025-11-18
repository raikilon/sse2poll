package ch.sse2poll.core;

import ch.sse2poll.core.engine.support.interfaces.AsyncRunner;
import ch.sse2poll.core.engine.support.interfaces.IdGenerator;
import ch.sse2poll.core.entities.model.Pending;
import ch.sse2poll.core.entities.model.Ready;
import ch.sse2poll.core.framework.annotation.PolledGet;
import ch.sse2poll.core.framework.config.Sse2PollAutoConfiguration;
import ch.sse2poll.core.framework.web.PolledGetAspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(Sse2PollCoreTest.TestConfig.class)
class Sse2PollCoreTest {

    @Autowired
    TestContext context;

    @BeforeEach
    void setUp() {
        context.reset();
    }

    @AfterEach
    void cleanup() {
        context.cleanup();
    }

    @Test
    void givenKickoff_WhenInvokingController_ThenPendingReturnedAndJobScheduled() {
        context.request(null, 0);

        Object response = context.controller.get();

        assertTrue(response instanceof Pending);
        assertEquals("job-1", context.idGenerator.lastGenerated());
        assertEquals(1, context.asyncRunner.pendingTasks());
        assertEquals(0, context.controller.invocationCount());
    }

    @Test
    void givenPollAfterCompletion_WhenInvokingController_ThenReadyReturnedAndCacheCleared() {
        context.request(null, 0);
        context.controller.get();
        String jobId = context.idGenerator.lastGenerated();
        context.asyncRunner.completeNext();

        context.request(jobId, 0);
        Ready<?> ready = assertInstanceOf(Ready.class, context.controller.get());
        assertEquals("payload", ready.payload());
        assertEquals(1, context.controller.invocationCount());

        context.request(jobId, 0);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> context.controller.get());
        assertTrue(ex.getMessage().contains("Unknown job id"));
    }

    @Test
    void givenKickoffWithWait_WhenComputationCompletesWithinWindow_ThenReadyResponseReturned() throws InterruptedException {
        Thread worker = new Thread(() -> context.asyncRunner.completeNextBlocking(Duration.ofSeconds(1)));
        worker.start();

        context.request(null, 500);
        Ready<?> ready = assertInstanceOf(Ready.class, context.controller.get());
        assertEquals("payload", ready.payload());
        assertEquals(1, context.controller.invocationCount());

        worker.join();
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Import({Sse2PollAutoConfiguration.class, PolledGetAspect.class})
    static class TestConfig {

        @Bean
        TestController testController() {
            return new TestController();
        }

        @Bean
        @Primary
        TrackingIdGenerator trackingIdGenerator() {
            return new TrackingIdGenerator();
        }

        @Bean
        @Primary
    FakeAsyncRunner fakeAsyncRunner() {
            return new FakeAsyncRunner();
        }

        @Bean
        TestContext testContext(TestController controller,
                                FakeAsyncRunner asyncRunner,
                                TrackingIdGenerator idGenerator) {
            return new TestContext(controller, asyncRunner, idGenerator);
        }
    }

    static class TestContext {
        final TestController controller;
        final FakeAsyncRunner asyncRunner;
        final TrackingIdGenerator idGenerator;

        TestContext(TestController controller, FakeAsyncRunner asyncRunner, TrackingIdGenerator idGenerator) {
            this.controller = controller;
            this.asyncRunner = asyncRunner;
            this.idGenerator = idGenerator;
        }

        void reset() {
            RequestContextHolder.resetRequestAttributes();
            controller.reset();
            asyncRunner.clear();
            idGenerator.reset();
        }

        void cleanup() {
            RequestContextHolder.resetRequestAttributes();
            asyncRunner.clear();
        }

        void request(String jobId, long waitMs) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            if (jobId != null) {
                request.setParameter("job", jobId);
            }
            request.setParameter("waitMs", Long.toString(waitMs));
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        }
    }

    static class TestController {
        private final AtomicInteger invocations = new AtomicInteger();

        @PolledGet
        Object get() {
            invocations.incrementAndGet();
            return "payload";
        }

        void reset() {
            invocations.set(0);
        }

        int invocationCount() {
            return invocations.get();
        }
    }

    static class TrackingIdGenerator implements IdGenerator {
        private final AtomicInteger counter = new AtomicInteger();
        private final AtomicReference<String> lastGenerated = new AtomicReference<>();

        @Override
        public synchronized String newId() {
            String id = "job-" + counter.incrementAndGet();
            lastGenerated.set(id);
            return id;
        }

        String lastGenerated() {
            return lastGenerated.get();
        }

        void reset() {
            counter.set(0);
            lastGenerated.set(null);
        }
    }

    /**
     * Fake AsyncRunner that queues computations until tests decide to execute them.
     */
    static class FakeAsyncRunner implements AsyncRunner {
        private final BlockingQueue<Task> tasks = new LinkedBlockingQueue<>();

        @Override
        public void run(Supplier<Object> compute, Consumer<Object> onSuccess) {
            tasks.add(new Task(compute, onSuccess));
        }

        int pendingTasks() {
            return tasks.size();
        }

        void completeNext() {
            Task task = tasks.poll();
            if (task == null) {
                throw new IllegalStateException("No async tasks scheduled");
            }
            task.run();
        }

        void completeNextBlocking(Duration timeout) {
            try {
                Task task = tasks.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (task == null) {
                    throw new IllegalStateException("Timed out waiting for async task");
                }
                task.run();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for async task", ie);
            }
        }

        void clear() {
            tasks.clear();
        }

        private record Task(Supplier<Object> compute, Consumer<Object> onSuccess) {
            void run() {
                Object payload = compute.get();
                onSuccess.accept(payload);
            }
        }
    }
}
