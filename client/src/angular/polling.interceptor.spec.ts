import { describe, expect, it, vi } from 'vitest';
import {
  HttpClient,
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpRequest,
  HttpResponse
} from '@angular/common/http';
import { lastValueFrom, of } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { pollingInterceptor } from './polling.interceptor';
import { withPolling } from './polling-context';

function makeRequest() {
  return new HttpRequest('GET', '/api/test', {
    context: withPolling({ pollIntervalMs: 0, maxPollAttempts: 2 })
  });
}

function runInterceptor(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) {
  return TestBed.runInInjectionContext(() =>
    pollingInterceptor(req, next)
  );
}

describe('pollingInterceptor', () => {
  it('passes through when polling not enabled', async () => {
    const req = new HttpRequest('GET', '/api/test');
    const next = vi.fn().mockReturnValue(
      of(new HttpResponse({ status: 200, body: { ok: true } }))
    );

    const result$ = runInterceptor(req, next);
    const result = await lastValueFrom(result$);

    expect(next).toHaveBeenCalled();
    expect((result as HttpResponse<any>).body).toEqual({ ok: true });
  });

  it('handles kickoff 202 and returns final 200 payload', async () => {
    const kickoff = new HttpResponse({ status: 202, body: { jobId: 'job-1' } });
    const next = vi.fn().mockReturnValue(of<HttpEvent<unknown>>(kickoff));

    const httpGet = vi.fn().mockReturnValue(
      of(new HttpResponse({ status: 200, body: { done: true } }))
    );
    TestBed.configureTestingModule({
      providers: [{ provide: HttpClient, useValue: { get: httpGet } }]
    });

    const req = makeRequest();
    const result$ = runInterceptor(req, next);
    const result = await lastValueFrom(result$);

    expect(httpGet).toHaveBeenCalledTimes(1);
    expect((result as HttpResponse<any>).status).toBe(200);
    expect((result as HttpResponse<any>).body).toEqual({ done: true });
  });

  it('returns kickoff event when jobId is missing', async () => {
    const kickoff = new HttpResponse({ status: 202, body: { noJob: true } });
    const next = vi.fn().mockReturnValue(of<HttpEvent<unknown>>(kickoff));
    TestBed.configureTestingModule({
      providers: [{ provide: HttpClient, useValue: { get: vi.fn() } }]
    });

    const req = makeRequest();
    const result$ = runInterceptor(req, next);
    const result = await lastValueFrom(result$);

    expect((result as HttpResponse<any>).status).toBe(202);
    expect((result as HttpResponse<any>).body).toEqual({ noJob: true });
  });

  it('surfaces polling failures as HttpErrorResponse', async () => {
    const kickoff = new HttpResponse({ status: 202, body: { jobId: 'job-1' } });
    const next = vi.fn().mockReturnValue(of<HttpEvent<unknown>>(kickoff));

    const httpGet = vi.fn().mockReturnValue(
      of(new HttpResponse({ status: 404, body: { message: 'missing' } }))
    );
    TestBed.configureTestingModule({
      providers: [{ provide: HttpClient, useValue: { get: httpGet } }]
    });

    const req = makeRequest();
    const result$ = runInterceptor(req, next);

    await expect(lastValueFrom(result$)).rejects.toBeInstanceOf(
      HttpErrorResponse
    );
  });
});
