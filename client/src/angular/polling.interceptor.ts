import { inject } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
  HttpResponse
} from '@angular/common/http';
import { from, of } from 'rxjs';
import { switchMap, finalize } from 'rxjs/operators';
import {
  POLLING_CONFIG,
  normalizePollingOptions
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

  const kickoffReq =
    normalized.waitMs === undefined
      ? req
      : req.clone({
          params: req.params.set(
            'waitMs',
            String(normalized.waitMs)
          )
        });

  return next(kickoffReq).pipe(
    switchMap(event => {
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
            url: req.url,
            jobId,
            ...normalized,
            headers: req.headers,
            withCredentials: req.withCredentials,
            context: req.context,
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
    })
  );
};
