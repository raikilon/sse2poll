import { inject } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpRequest,
  HttpResponse,
  HttpInterceptorFn
} from '@angular/common/http';
import { from, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { POLLING_CONFIG } from './polling-context';
import { PollingOrchestrator } from './polling-orchestrator';

export const pollingInterceptor: HttpInterceptorFn = (req, next) => {
  const orchestrator = new PollingOrchestrator(inject(HttpClient));
  const cfg = req.context.get(POLLING_CONFIG);
  if (!cfg.enabled) {
    return next(req);
  }

  const normalized = normalizeConfig(cfg);
  const kickoffReq = appendWaitMs(req, normalized.waitMs);

  return next(kickoffReq).pipe(
    switchMap(event => {
      if (!(event instanceof HttpResponse)) {
        return of(event);
      }
      if (event.status === 200) {
        return of(event);
      }
      if (event.status !== 202) {
        return of(event);
      }

      const jobId = extractJobId(event.body);
      if (!jobId) {
        return of(event);
      }

      const promise = orchestrator
        .pollUntilReady<any>({
          url: req.urlWithParams,
          jobId,
          waitMs: normalized.waitMs,
          pollIntervalMs: normalized.pollIntervalMs,
          maxPollAttempts: normalized.maxPollAttempts,
          headers: req.headers,
          withCredentials: req.withCredentials,
          context: req.context
        })
        .then(result => new HttpResponse({ status: 200, body: result.payload, url: req.urlWithParams }))
        .catch(error => {
          const status = (error as any)?.status ?? 0;
          const statusText = (error as any)?.statusText ?? 'Polling failed';
          throw new HttpErrorResponse({
            error,
            status,
            statusText,
            url: req.urlWithParams
          });
        });

      return from(promise);
    })
  );
};

function appendWaitMs(req: HttpRequest<unknown>, waitMs?: number): HttpRequest<unknown> {
  if (waitMs === undefined) {
    return req;
  }
  return req.clone({
    params: req.params.set('waitMs', String(Math.max(0, waitMs)))
  });
}

function extractJobId(body: unknown): string | undefined {
  if (body && typeof body === 'object' && 'jobId' in body) {
    const candidate = (body as Record<string, unknown>).jobId;
    if (typeof candidate === 'string' && candidate.trim().length > 0) {
      return candidate;
    }
  }
  return undefined;
}

function normalizeConfig(cfg: { waitMs?: number; pollIntervalMs?: number; maxPollAttempts?: number | null }) {
  return {
    waitMs: cfg.waitMs === undefined ? undefined : Math.max(0, cfg.waitMs),
    pollIntervalMs: Math.max(0, cfg.pollIntervalMs ?? 250),
    maxPollAttempts: cfg.maxPollAttempts === null ? null : cfg.maxPollAttempts ?? 60
  };
}
