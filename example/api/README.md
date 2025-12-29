# API demo (Spring Boot)

Sample Spring Boot app that exposes a polled endpoint to demonstrate `@PolledGet`.

## Run
From repo root:
```bash
cd example/api
mvn spring-boot:run 
```

## Endpoint
- `GET /api/catalog/products/{productId}` supports `waitMs` and `job` query params.
  - Kickoff: call without `job`. The controller is wrapped by `@PolledGet`; a job id is created and the computation runs on a virtual thread.
  - If the payload is not ready, you receive `202 { "jobId": "<id>" }`; when ready, the same call returns `200` with the product JSON.
  - Poll with `?job=<id>` (and optional `waitMs`) to re-check readiness. A missing/expired job returns 404.
- Product ids: `keyboard`, `mouse`, `monitor`, `dock` (backed by a fake slow store).

## Test with HTTP requests
Use `example/api/catalog.http` in VS Code REST Client / IntelliJ HTTP client:
- Send the kickoff request (first block) to get a `jobId`.
- Paste that id into the second request and re-run to poll until you see `200`.

