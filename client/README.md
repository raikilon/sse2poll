# SSE2Poll Angular Polling Interceptor

Lightweight Angular-only client for the SSE2Poll protocol. Opt in per request with `withPolling`; the interceptor handles `202 { jobId }` responses and polls until the payload is ready. Tested with Angular 21 (compatible with other 17+ standalone apps).

## Install from this repo
1. `cd client && npm install` (installs `@angular/compiler-cli` for the library build)
2. `npm run build && npm pack` (Angular compiler `ngc` produces `sse2poll-polling-client-0.1.2.tgz`)
3. In your Angular app: `npm install ../client/sse2poll-polling-client-0.1.2.tgz` (repack if you change the library so the tarball matches your dependency).

## Wire it up
```ts
import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { pollingInterceptor } from '@sse2poll/polling-client/angular';

bootstrapApplication(AppComponent, {
  providers: [provideHttpClient(withInterceptors([pollingInterceptor]))]
});
```
NgModule-based apps can use the same `provideHttpClient(withInterceptors([...]))` inside their bootstrap or module providers.

## Opt in on a request
```ts
import { withPolling } from '@sse2poll/polling-client/angular';

this.http.get<MyDto>('/api/report', { context: withPolling({ waitMs: 1000 }) })
  .subscribe(result => console.log(result));
```
- If the kickoff responds with `202 { jobId }`, the interceptor polls the same URL with `job` (and `waitMs` if set) until a `200` payload arrives, then emits a single `HttpResponse`.
- Responses that are already `200` pass through unchanged; 404/limit errors surface as `HttpErrorResponse`.

## Options via `withPolling`
- `waitMs`: optional server-side wait on kickoff/poll calls (omitted if undefined).
- `pollIntervalMs`: delay between polls after a 202 (default 250ms).
- `maxPollAttempts`: cap before failing (default 60; set `null` for unlimited).
- Use `disablePolling()` on a request context to explicitly opt out.

## Tests
- Run `npm test` (Vitest + HttpClientTestingModule; no backend needed).
- If npm warns about Angular/zone.js peer ranges, align devDependencies with Angular 21.0.3+ and zone.js 0.16.x.
