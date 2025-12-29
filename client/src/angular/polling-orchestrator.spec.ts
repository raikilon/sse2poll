import { describe, expect, it, vi } from 'vitest';
import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { firstValueFrom, of, throwError } from 'rxjs';
import { PollingOrchestrator } from './polling-orchestrator';
import { PollingError } from './polling-utils';

// Helper to build HttpResponse easily
function response<T>(status: number, body: T, url = '/api/test') {
  return new HttpResponse<T>({ status, body, url });
}

describe('PollingOrchestrator', () => {
  const url = '/api/test';
  const baseOpts = {
    url,
    jobId: 'job-1',
    pollIntervalMs: 0,
    maxPollAttempts: 3
  } as const;

  it('returns payload when first poll is ready', async () => {
    const http = mockHttp([
      response(200, { value: 42 })
    ]);
    const orchestrator = new PollingOrchestrator(http as unknown as HttpClient);

    const result = await orchestrator.pollUntilReady<{ value: number }>(baseOpts);

    expect(result.payload).toEqual({ value: 42 });
    expect(result.attempt).toBe(0);
  });

  it('polls until ready and tracks attempts', async () => {
    const http = mockHttp([
      response(202, { jobId: 'job-1' }),
      response(202, { jobId: 'job-1' }),
      response(200, { done: true })
    ]);
    const orchestrator = new PollingOrchestrator(http as unknown as HttpClient);

    const result = await orchestrator.pollUntilReady<{ done: boolean }>({
      ...baseOpts,
      pollIntervalMs: 0
    });

    expect(result.payload).toEqual({ done: true });
    expect(result.attempt).toBe(2);
  });

  it('updates jobId if backend rotates it on 202', async () => {
    const http = mockHttp([
      response(202, { jobId: 'job-1' }),
      response(202, { jobId: 'job-2' }),
      response(200, { done: true })
    ]);
    const orchestrator = new PollingOrchestrator(http as unknown as HttpClient);

    const result = await orchestrator.pollUntilReady<{ done: boolean }>(baseOpts);

    expect(result.jobId).toBe('job-2');
  });

  it('throws when job is missing (404)', async () => {
    const http = mockHttp([response(404, { message: 'missing' })]);
    const orchestrator = new PollingOrchestrator(http as unknown as HttpClient);

    await expect(orchestrator.pollUntilReady(baseOpts)).rejects.toThrow(
      new PollingError('Job job-1 not found', 404, url)
    );
  });

  it('throws on unexpected status', async () => {
    const http = mockHttp([response(500, { oops: true })]);
    const orchestrator = new PollingOrchestrator(http as unknown as HttpClient);

    await expect(orchestrator.pollUntilReady(baseOpts)).rejects.toThrow(
      new PollingError('Polling failed', 500, url, { oops: true })
    );
  });

  it('aborts when signal is aborted', async () => {
    const controller = new AbortController();
    const http = mockHttp(() => {
      controller.abort();
      return response(202, { jobId: 'job-1' });
    });
    const orchestrator = new PollingOrchestrator(http as unknown as HttpClient);

    await expect(
      orchestrator.pollUntilReady({ ...baseOpts, signal: controller.signal })
    ).rejects.toThrow(new PollingError('Polling aborted', 499, url));
  });

  it('honors waitMs by passing waitMs param', async () => {
    const spyGet = vi.fn().mockReturnValue(of(response(202, { jobId: 'job-1' })));
    const http = { get: spyGet } as unknown as HttpClient;
    const orchestrator = new PollingOrchestrator(http);

    await expect(
      orchestrator.pollUntilReady({ ...baseOpts, waitMs: 123 })
    ).rejects.toBeInstanceOf(PollingError); // fails after attempts, but we assert param usage below

    expect(spyGet).toHaveBeenCalled();
    const call = spyGet.mock.calls[0][1];
    const params = (call.params as HttpParams);
    expect(params.get('waitMs')).toBe('123');
  });

  it('stops after max attempts and throws', async () => {
    const http = mockHttp([
      response(202, { jobId: 'job-1' }),
      response(202, { jobId: 'job-1' }),
      response(202, { jobId: 'job-1' }),
      response(202, { jobId: 'job-1' })
    ]);
    const orchestrator = new PollingOrchestrator(http as unknown as HttpClient);

    await expect(orchestrator.pollUntilReady(baseOpts)).rejects.toThrow(
      new PollingError('Polling exceeded 3 attempts', 504, url)
    );
  });
});

function mockHttp(responses: Array<HttpResponse<any>> | (() => HttpResponse<any>)) {
  let call = 0;
  const next = () =>
    typeof responses === 'function' ? responses() : responses[call++] ?? responses[responses.length - 1];

  return {
    get: vi.fn().mockImplementation(() => {
      const res = next();
      if (res instanceof Error) {
        return throwError(() => res);
      }
      return of(res);
    })
  };
}
