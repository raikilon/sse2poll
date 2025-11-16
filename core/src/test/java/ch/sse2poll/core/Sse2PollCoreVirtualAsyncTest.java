package ch.sse2poll.core;

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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(Sse2PollCoreVirtualAsyncTest.TestConfig.class)
class Sse2PollCoreVirtualAsyncTest {

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
    void givenVirtualThreadAsyncRunner_WhenPollingWithWait_ThenReadyArrivesAfterComputationCompletes() throws Exception {
        context.request(null, 0);
        Object kickoff = context.controller.get();

        assertTrue(kickoff instanceof Pending);
        String jobId = context.idGenerator.lastGenerated();

        context.controller.awaitInvocation();

        Thread completer = new Thread(() -> {
            try {
                Thread.sleep(150);
                context.controller.releaseComputation();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        completer.start();

        context.request(jobId, 1000);
        Ready<?> ready = assertInstanceOf(Ready.class, context.controller.get());
        assertEquals("payload", ready.payload());
        context.controller.awaitCompletion();

        completer.join();
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
        TestContext testContext(TestController controller, TrackingIdGenerator idGenerator) {
            return new TestContext(controller, idGenerator);
        }
    }

    static class TestContext {
        final TestController controller;
        final TrackingIdGenerator idGenerator;

        TestContext(TestController controller, TrackingIdGenerator idGenerator) {
            this.controller = controller;
            this.idGenerator = idGenerator;
        }

        void reset() {
            RequestContextHolder.resetRequestAttributes();
            controller.reset();
            idGenerator.reset();
        }

        void cleanup() {
            RequestContextHolder.resetRequestAttributes();
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

        private CountDownLatch invocationLatch = new CountDownLatch(1);
        private CountDownLatch releaseLatch = new CountDownLatch(1);
        private CountDownLatch completionLatch = new CountDownLatch(1);

        @PolledGet
        Object get() {
            invocations.incrementAndGet();
            invocationLatch.countDown();
            awaitLatch(releaseLatch);
            completionLatch.countDown();
            return "payload";
        }

        void reset() {
            invocations.set(0);
            invocationLatch = new CountDownLatch(1);
            releaseLatch = new CountDownLatch(1);
            completionLatch = new CountDownLatch(1);
        }

        void awaitInvocation() throws InterruptedException {
            invocationLatch.await(2, TimeUnit.SECONDS);
        }

        void releaseComputation() {
            releaseLatch.countDown();
        }

        void awaitCompletion() throws InterruptedException {
            completionLatch.await(2, TimeUnit.SECONDS);
        }

        private void awaitLatch(CountDownLatch latch) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for test latch", e);
            }
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
}
