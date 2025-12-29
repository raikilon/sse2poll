import { inject } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpEvent,
  HttpRequest,
  HttpResponse
} from '@angular/common/http';
import { Observable, from, of } from 'rxjs';
import { switchMap, finalize } from 'rxjs/operators';
import {
  POLLING_CONFIG,
  normalizePollingOptions,
  type NormalizedPollingOptions
} from './polling-context';
import { PollingOrchestrator } from './polling-orchestrator';
import { extractJobId, PollingError } from './polling-utils';

export const pollingInterceptor: HttpInterceptorFn = (req, next) => {
  const config = req.context.get(POLLING_CONFIG);
  if (!config.enabled) {
    return next(req);
  }

  const http = inject(HttpClient);
  const orchestrator = new PollingOrchestrator(http);

  const normalized = normalizePollingOptions(config);

  const kickoffReq = addWaitMsIfNeeded(req, normalized.waitMs);

  return next(kickoffReq).pipe(
    switchMap((event: HttpEvent<unknown>) =>
      handleKickoffResponse(event, req, normalized, orchestrator)
    )
  );
};

function addWaitMsIfNeeded(
  req: HttpRequest<unknown>,
  waitMs?: number
): HttpRequest<unknown> {
  if (waitMs === undefined) {
    return req;
  }
  return req.clone({
    params: req.params.set('waitMs', String(waitMs))
  });
}

function handleKickoffResponse(
  event: HttpEvent<unknown>,
  originalReq: HttpRequest<unknown>,
  normalized: NormalizedPollingOptions,
  orchestrator: PollingOrchestrator
): Observable<HttpEvent<unknown>> {
  if (!(event instanceof HttpResponse) || event.status !== 202) {
    return of(event);
  }

  const jobId = extractJobId(event.body);
  if (!jobId) {
    return of(event);
  }

  const controller = new AbortController();

  return from(
    orchestrator
      .pollUntilReady<any>({
        url: originalReq.url,
        jobId,
        ...normalized,
        headers: originalReq.headers,
        withCredentials: originalReq.withCredentials,
        context: originalReq.context,
        signal: controller.signal
      })
      .then(result =>
        event.clone({
          status: 200,
          body: result.payload
        })
      )
      .catch((error: PollingError) => {
        throw new HttpErrorResponse({
          error,
          status: error.status,
          url: error.url
        });
      })
  ).pipe(finalize(() => controller.abort()));
}
