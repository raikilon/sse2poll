import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { pollingInterceptor } from './polling.interceptor';
import { withPolling } from './polling-context';

const flushMicrotasks = () => Promise.resolve();

describe('PollingInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([pollingInterceptor])),
        provideHttpClientTesting()
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      httpMock.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('returns ready payload when kickoff responds 200', async () => {
    const promise = firstValueFrom(
      http.get<{ value: string }>('/api/report', { context: withPolling() })
    );

    const kickoff = httpMock.expectOne(req => req.urlWithParams === '/api/report');
    kickoff.flush({ value: 'done' });

    await expect(promise).resolves.toEqual({ value: 'done' });
  });

  it('polls until ready after 202 kickoff', async () => {
    const promise = firstValueFrom(
      http.get<{ value: string }>('/api/report', { context: withPolling({ pollIntervalMs: 0 }) })
    );

    const kickoff = httpMock.expectOne('/api/report');
    kickoff.flush({ jobId: 'abc' }, { status: 202, statusText: 'Accepted' });

    await flushMicrotasks();

    const poll1 = httpMock.expectOne(req => req.urlWithParams === '/api/report?job=abc');
    poll1.flush({ jobId: 'abc' }, { status: 202, statusText: 'Accepted' });

    await flushMicrotasks();

    const poll2 = httpMock.expectOne(req => req.urlWithParams === '/api/report?job=abc');
    poll2.flush({ value: 'done' }, { status: 200, statusText: 'OK' });

    await expect(promise).resolves.toEqual({ value: 'done' });
  });

  it('omits waitMs when not provided', async () => {
    const promise = firstValueFrom(
      http.get('/api/report', { context: withPolling({ pollIntervalMs: 0 }) })
    );

    const kickoff = httpMock.expectOne(req => req.urlWithParams === '/api/report');
    kickoff.flush({ jobId: 'abc' }, { status: 202, statusText: 'Accepted' });

    await flushMicrotasks();

    const poll = httpMock.expectOne(req => req.urlWithParams === '/api/report?job=abc');
    poll.flush({ value: 1 }, { status: 200, statusText: 'OK' });

    await promise;
  });

  it('propagates waitMs when provided', async () => {
    const promise = firstValueFrom(
      http.get('/api/report', { context: withPolling({ waitMs: 500, pollIntervalMs: 0 }) })
    );

    const kickoff = httpMock.expectOne(req => req.urlWithParams === '/api/report?waitMs=500');
    kickoff.flush({ jobId: 'abc' }, { status: 202, statusText: 'Accepted' });

    await flushMicrotasks();

    const poll = httpMock.expectOne(req => req.urlWithParams === '/api/report?job=abc&waitMs=500');
    poll.flush({ value: 1 }, { status: 200, statusText: 'OK' });

    await promise;
  });

  it('surfaces 404 unknown job as HttpErrorResponse', async () => {
    const promise = firstValueFrom(
      http.get('/api/report', { context: withPolling({ pollIntervalMs: 0 }) })
    );

    const kickoff = httpMock.expectOne('/api/report');
    kickoff.flush({ jobId: 'missing' }, { status: 202, statusText: 'Accepted' });

    await flushMicrotasks();

    const poll = httpMock.expectOne('/api/report?job=missing');
    poll.flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });

    await expect(promise).rejects.toBeInstanceOf(HttpErrorResponse);
  });

  it('respects maxPollAttempts', async () => {
    const promise = firstValueFrom(
      http.get('/api/report', { context: withPolling({ pollIntervalMs: 0, maxPollAttempts: 1 }) })
    );

    const kickoff = httpMock.expectOne('/api/report');
    kickoff.flush({ jobId: 'abc' }, { status: 202, statusText: 'Accepted' });

    await flushMicrotasks();

    const poll = httpMock.expectOne('/api/report?job=abc');
    poll.flush({ jobId: 'abc' }, { status: 202, statusText: 'Accepted' });

    await expect(promise).rejects.toBeInstanceOf(HttpErrorResponse);
  });
});
