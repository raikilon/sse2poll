package ch.sse2poll.core.framework.config;

import ch.sse2poll.core.engine.CacheBackedPollCoordinator;
import ch.sse2poll.core.engine.port.incoming.PollCoordinator;
import ch.sse2poll.core.engine.port.outgoing.CacheClient;
import ch.sse2poll.core.engine.support.implementation.DefaultKeyFactory;
import ch.sse2poll.core.engine.support.implementation.PollingReadyAwaiter;
import ch.sse2poll.core.engine.support.implementation.UuidIdGenerator;
import ch.sse2poll.core.engine.support.implementation.VirtualThreadAsyncRunner;
import ch.sse2poll.core.engine.support.interfaces.AsyncRunner;
import ch.sse2poll.core.engine.support.interfaces.IdGenerator;
import ch.sse2poll.core.engine.support.interfaces.KeyFactory;
import ch.sse2poll.core.engine.support.interfaces.ReadyAwaiter;
import ch.sse2poll.core.framework.cache.CaffeineCacheClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that wires the {@link CacheBackedPollCoordinator} with default collaborators.
 */
@Configuration
public class Sse2PollAutoConfiguration {

    @Bean
    public IdGenerator pollCoordinatorIdGenerator() {
        return new UuidIdGenerator();
    }

    @Bean
    public KeyFactory pollCoordinatorKeyFactory() {
        return new DefaultKeyFactory();
    }

    @Bean
    public ReadyAwaiter pollCoordinatorReadyAwaiter() {
        return new PollingReadyAwaiter();
    }

    @Bean
    public AsyncRunner pollCoordinatorAsyncRunner() {
        return new VirtualThreadAsyncRunner();
    }

    @Bean
    public CacheClient caffeineCacheClient() {
        return new CaffeineCacheClient(10_000);
    }

    @Bean
    public PollCoordinator cacheBackedPollCoordinator(CacheClient cacheClient,
                                                      IdGenerator idGenerator,
                                                      KeyFactory keyFactory,
                                                      ReadyAwaiter readyAwaiter,
                                                      AsyncRunner asyncRunner) {
        return new CacheBackedPollCoordinator(cacheClient, idGenerator, keyFactory, readyAwaiter, asyncRunner);
    }
}
