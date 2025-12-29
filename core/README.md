# SSE2Poll Core (Java)

Spring library that turns slow GET endpoints into a pollable workflow. Annotate a controller with `@PolledGet` and the aspect will handle kickoff (`202 { jobId }`), cache the pending job, and serve the final payload on subsequent polls.

## Requirements
- Java 21+ (virtual threads used for async execution; builds target `release 25`).
- Spring Boot / Spring 6 with AOP enabled (`spring-boot-starter-aop`).

## Build and install locally
```bash
cd core
mvn clean install   # installs ch.sse2poll:core:1.0-SNAPSHOT to your local Maven repo
```
Then declare the dependency in your app:
```xml
<dependency>
  <groupId>ch.sse2poll</groupId>
  <artifactId>core</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

## Wire it into Spring Boot
```java
@SpringBootApplication
@Import({Sse2PollAutoConfiguration.class, PolledGetAspect.class})
class Application { }
```

Annotate slow GET endpoints:
```java
@RestController
@RequestMapping("/api/catalog")
class CatalogController {
  @PolledGet
  @GetMapping("/products/{productId}")
  ProductDetails getProduct(@PathVariable String productId) {
    return slowLookup(productId);
  }
}
```
- Kickoff: call without `job`. If the work is still running, you get `202 { "jobId": "<id>" }`.
- Poll: call the same URL with `?job=<id>` (and optional `waitMs`) to reuse the cached job; returns `200` when ready, `202` while pending, `404` if missing/expired.
- `waitMs` lets the server wait before falling back to `202`, reducing needless polls for short jobs.

## Defaults and customization
`Sse2PollAutoConfiguration` wires:
- `CacheClient`: Caffeine cache (`10k` entries, `5m` TTL).
- `IdGenerator`: UUID strings.
- `AsyncRunner`: virtual threads for the computation.
- `ReadyAwaiter`: server-side wait loop honoring `waitMs`.
- `KeyFactory`: combines controller namespace and job id.

The Caffeine cache is used for simplicity in the demo; in real deployments you likely want a distributed store (e.g., a Redis-backed `CacheClient`) so multiple pods share the same job state. Override any of these beans to plug in your own storage or async runner.

## How it works (flow)
1) Kickoff request hits the `@PolledGet` aspect.
2) A job id is generated and a `Pending` envelope is cached; the computation runs async.
3) The aspect optionally waits up to `waitMs` for readiness; returns `200` if done, else `202 { jobId }`.
4) Polling with `job` checks the same cache entry until it is `Ready`, then deletes it and returns the payload.
