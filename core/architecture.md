# ch.sse2poll.core — Library structure & skeleton

A Spring Boot–friendly library that lets you replace Server-Sent Events (SSE) with robust long‑polling using Caffeine (in‑memory) and optionally Redis for cross‑node fan‑out.

---

## Package layout

```
ch.sse2poll.core
├─ application                      # "Use cases" — orchestrate business flow
│   ├─ PollCoordinator.java         // interface (use case boundary)
│   ├─ DefaultPollCoordinator.java  // implementation (uses CacheGateway + JobIdGenerator)
│   └─ commands/
│       ├─ StartJobCommand.java     // kickoff flow logic
│       └─ PollJobCommand.java      // follow-up polling logic
│
├─ domain                           # "Entities" — pure logic, independent of frameworks
│   ├─ model/
│   │   ├─ Envelope.java            // sealed interface
│   │   ├─ Ready.java
│   │   └─ Pending.java
│   ├─ service/
│   │   ├─ IdGenerator.java         // ULID/UUIDv7
│   │   └─ JobKeyFactory.java       // builds s2p:<namespace>:<jobId>
│   └─ entity/
│       ├─ Job.java                 // aggregates jobId, status, timestamps
│       └─ Namespace.java           // value object for controller+method scope
│
├─ interfaceadapters                # "Gateways" (infra) & Presenters
│   ├─ gateways/
│   │   ├─ CacheClient.java         // interface
│   │   ├─ RedisCacheClient.java
│   │   ├─ CaffeineCacheClient.java
│   │   └─ Serializer.java
│   ├─ presenters/
│   │   ├─ Responses.java           // HTTP response mapping (200/202/204)
│   │   └─ PollPresenter.java       // orchestrates Envelope → Response
│   └─ web/
│       ├─ PolledGetAspect.java     // annotation adapter
│       ├─ RequestContext.java
│       └─ WebUtil.java
│
├─ framework                        # External frameworks, config, and bootstrapping
│   ├─ annotation/
│   │   └─ PolledGet.java
│   ├─ config/
│   │   ├─ Sse2PollAutoConfiguration.java
│   │   └─ Sse2PollProperties.java
│   └─ infra/
│       ├─ redis/
│       │   ├─ RedisConfig.java
│       │   └─ RedisKeys.java
│       └─ caffeine/
│           └─ CaffeineConfig.java
│
└─ util
    └─ JacksonSerializer.java

```

---

## Developer experience (what users write)

```java
@RestController
@RequestMapping("/reports")
class ReportsController {

  @GetMapping("/daily")
  @PolledGet(ttl = 300, pendingTtl = 30) // minimal annotation
  public ReportDto getDailyReport() {
      // potentially slow computation
      return service.buildDailyReportForCurrentUser();
  }
}
```

**Client contract**

* Kickoff: `GET /reports/daily?waitMs=2000`

  * If computation finishes fast → **200** with `{ payload }` (⚠️ no jobId returned).
  * If not finished within wait window → **202** `{ jobId, status: "pending" }`.
* Poll: `GET /reports/daily?job=<jobId>&waitMs=2000`

  * When ready → **200** `{ payload }`.
  * While pending → **202** `{ jobId, status: "pending" }`.

Defaults: `waitMsDefault=3000`, max cap `maxWaitMs=30000`. Both are properties, **not** annotation params.

---

## API models

```java
// Models.java
public sealed interface Envelope permits Ready, Pending {}
public record Ready<T>(long ts, T payload) implements Envelope {}
public record Pending(long startedAt) implements Envelope {}
```

```java
// CacheClient.java (backend SPI)
public interface CacheClient {
  java.util.Optional<Envelope> read(String key, Class<?> bodyType);
  void writePending(String key, java.time.Duration pendingTtl);
  void writeReady(String key, Object payload, java.time.Duration valueTtl);
  default void touch(String key, java.time.Duration ttl) {}
}
```

```java
// PollCoordinator.java
public interface PollCoordinator {
  Object handle(String namespace, PolledGet ann,
                java.util.function.Supplier<Object> compute,
                RequestContext ctx);
}
```

---

## Annotation (minimal surface)

```java
// PolledGet.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PolledGet {
  int ttl() default 300;        // READY retention (seconds)
  int pendingTtl() default 30;  // PENDING retention (seconds)
}
```

* **No stream/key params.**
* Namespace is **auto**: `<DeclaringClassSimpleName>#<methodName>`.
* Request params are **fixed**:

  * `job` (optional): jobId for follow-ups
  * `waitMs` (optional): per-call wait override

---

## Web/AOP integration

```java
// PolledGetAspect.java
@org.aspectj.lang.annotation.Aspect
@org.springframework.stereotype.Component
final class PolledGetAspect {
  private final PollCoordinator coordinator;

  PolledGetAspect(PollCoordinator coordinator) { this.coordinator = coordinator; }

  @org.aspectj.lang.annotation.Around("@annotation(ann)")
  public Object around(org.aspectj.lang.ProceedingJoinPoint pjp, PolledGet ann) throws Throwable {
    var method = ((org.aspectj.lang.reflect.MethodSignature) pjp.getSignature()).getMethod();
    String namespace = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    var ctx = RequestContext.current();
    return coordinator.handle(namespace, ann, () -> {
      try { return pjp.proceed(); } catch (Throwable t) { throw new RuntimeException(t); }
    }, ctx);
  }
}
```

---

## Coordinator (lock‑free, job‑ID flow)

```java
// DefaultPollCoordinator.java
@lombok.RequiredArgsConstructor
public class DefaultPollCoordinator implements PollCoordinator {
  private final CacheClient cache;
  private final java.util.concurrent.ExecutorService vt; // virtual threads
  private final Sse2PollProperties props; // waitMsDefault, maxWaitMs, statusOnTimeout, key prefix

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
    var future = java.util.concurrent.CompletableFuture.supplyAsync(compute, vt)
      .whenComplete((body, err) -> {
        if (err == null) cache.writeReady(key, body, java.time.Duration.ofSeconds(ann.ttl()));
      });

    try {
      Object body = future.get(waitMs, java.util.concurrent.TimeUnit.MILLISECONDS);
      return Responses.ok(body); // finished fast → payload only, no jobId
    } catch (Exception e) {
      return Responses.accepted(java.util.Map.of("jobId", jobId, "status", "pending"));
    }
  }

  private Object pollExisting(String key, PolledGet ann, int waitMs) {
    var env = cache.read(key, Object.class);
    if (env.isPresent() && env.get() instanceof Ready<?> r) return Responses.ok(r.payload());
    long end = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(waitMs);
    while (System.nanoTime() < end) {
      var e2 = cache.read(key, Object.class);
      if (e2.isPresent() && e2.get() instanceof Ready<?> rr) return Responses.ok(rr.payload());
      java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(150));
    }
    return Responses.timeout(props.getStatusOnTimeout()); // 202 or 204
  }

  private static int clamp(int v, int lo, int hi){ return Math.max(lo, Math.min(hi, v)); }
  private String key(String ns, String job){ return props.getCache().getPrefix() + ":" + ns + ":" + job; }
}
```

---

## Cache backends

**RedisCacheClient** and **CaffeineCacheClient** both store the same JSON envelope under the generated key. Choose backend via properties.

```yaml
sse2poll:
  cache:
    backend: redis       # redis | caffeine
    prefix: s2p          # key prefix before namespace
    ttlSeconds: 300
    pendingTtlSeconds: 30
  waitMsDefault: 3000
  maxWaitMs: 30000
  statusOnTimeout: 202   # 202 or 204
  redis:
    uri: redis://localhost:6379
  caffeine:
    maxEntries: 10000
    expireAfterAccessSeconds: 600
```

---

## Auto‑configuration (selection: Redis or Caffeine)

```java
// Sse2PollAutoConfiguration.java (excerpt)
@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(Sse2PollProperties.class)
public class Sse2PollAutoConfiguration {
  @Bean ExecutorService virtualThreadExecutor(){ return java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor(); }
  @Bean Serializer serializer(){ return new JacksonSerializer(); }

  @Bean @ConditionalOnMissingBean(CacheClient.class)
  @ConditionalOnProperty(name="sse2poll.cache.backend", havingValue="redis", matchIfMissing=true)
  CacheClient redisOnly(org.springframework.data.redis.core.StringRedisTemplate t, Sse2PollProperties p, Serializer s) {
    return new RedisCacheClient(t, s, p.getCache().getPrefix());
  }

  @Bean @ConditionalOnProperty(name="sse2poll.cache.backend", havingValue="caffeine")
  CacheClient caffeineOnly(CaffeineCacheClient caffeine) { return caffeine; }

  @Bean PollCoordinator coordinator(CacheClient client, ExecutorService vt, Sse2PollProperties props){
    return new DefaultPollCoordinator(client, vt, props);
  }

  @Bean PolledGetAspect aspect(PollCoordinator coord){ return new PolledGetAspect(coord); }
}
```
