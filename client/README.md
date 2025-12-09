# SSE2Poll Angular Polling Interceptor

Lightweight Angular-only client for the SSE2Poll protocol. You opt in per request using `withPolling`, and the interceptor handles `202 {jobId}` responses by polling until a `200` payload is ready. If you omit `waitMs`, the server-side default is used (no query param is added). Tested with Angular 21 (compatible with other 21.x apps).

## Angular usage (interceptor, Angular 17+/standalone-style)
Provide the interceptor with the modern `provideHttpClient` API (Angular 17+):
```ts
import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import { PollingInterceptor } from '@sse2poll/polling-client/angular';

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: PollingInterceptor, multi: true }
  ]
});
```

Or classic DI (HTTP_INTERCEPTORS) if you prefer; both work.

Opt in on specific calls using `withPolling`:
```ts
import { withPolling } from '@sse2poll/polling-client/angular';

this.http.get<MyDto>('/api/report', { context: withPolling({ waitMs: 1000 }) })
  .subscribe(result => {
    // Fires only when the backend is ready (HTTP 200)
    console.log(result);
  });
```

Behavior:
- Kickoff request is sent; if it returns `202 { jobId }`, the interceptor keeps polling `?job=<id>` (plus `waitMs` if you provided it) until a `200` payload arrives, then emits a single `HttpResponse` to the caller.
- Errors surface as `HttpErrorResponse`.

Config via `withPolling`:
- `waitMs`: optional. If provided, it is sent on kickoff/poll requests; otherwise the backend default applies.
- `pollIntervalMs`: delay between polls after a 202 (default 250).
- `maxPollAttempts`: safety cap (default 60; set `null` for unlimited).

## Tests
- Run `npm test` (uses Vitest + HttpClientTestingModule with mocked responses; no backend needed).
  - If npm warns about Angular/zone.js versions, ensure devDependencies use Angular 21.0.3+ and zone.js 0.16.x to satisfy peer requirements.

## Files
- `src/angular/polling-context.ts`: opt-in helpers for Angular requests.
- `src/angular/polling.interceptor.ts`: Angular interceptor that blocks until ready.
- `src/angular/polling-orchestrator.ts`: polling loop used by the interceptor (HttpClient-based).
