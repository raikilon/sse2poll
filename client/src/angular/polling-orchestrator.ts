import { HttpClient, HttpContext, HttpHeaders, HttpResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { disablePolling } from './polling-context';

export interface PollingOrchestratorOptions {
  url: string;
  jobId: string;
  waitMs?: number;
  pollIntervalMs?: number;
  maxPollAttempts?: number | null;
  headers?: HttpHeaders;
  withCredentials?: boolean;
  context?: HttpContext;
}

export interface PollingResult<T> {
  jobId: string;
  attempt: number;
  payload: T;
}


export class PollingOrchestrator {
  constructor(private readonly http: HttpClient) {}

  async pollUntilReady<T>(options: PollingOrchestratorOptions): Promise<PollingResult<T>> {
    const waitMs = options.waitMs;
    const pollIntervalMs = Math.max(0, options.pollIntervalMs ?? 250);
    const maxPollAttempts = options.maxPollAttempts === null ? null : options.maxPollAttempts ?? 60;

    let jobId = options.jobId;
    let attempt = 0;

    while (maxPollAttempts === null || attempt < maxPollAttempts) {
      const response = await this.issuePoll<T>({
        url: options.url,
        jobId,
        waitMs,
        headers: options.headers,
        withCredentials: options.withCredentials,
        context: options.context
      });

      if (response.status === 200) {
        return { jobId, attempt, payload: response.body as T };
      }

      if (response.status === 202) {
        const nextJob = extractJobId(response.body);
        if (nextJob) {
          jobId = nextJob;
        }
        attempt++;
        if (pollIntervalMs > 0) {
          await sleep(pollIntervalMs);
        }
        continue;
      }

      if (response.status === 404) {
        throw buildError(`Job ${jobId} was not found while polling.`, 404, options.url);
      }

      throw buildError('Polling failed.', response.status, options.url, response.body);
    }

    throw buildError(
      `Polling exceeded the configured limit of ${maxPollAttempts} attempts for job ${jobId}.`,
      504,
      options.url
    );
  }

  private async issuePoll<T>(options: {
    url: string;
    jobId: string;
    waitMs?: number;
    headers?: HttpHeaders;
    withCredentials?: boolean;
    context?: HttpContext;
  }): Promise<HttpResponse<T | any>> {
    const params = new URLSearchParams();
    params.set('job', options.jobId);
    if (options.waitMs !== undefined) {
      params.set('waitMs', String(Math.max(0, options.waitMs)));
    }

    const target = appendParams(options.url, params);

    return firstValueFrom(
      this.http.request<T>('GET', target, {
        observe: 'response',
        responseType: 'json' as const,
        headers: options.headers,
        withCredentials: options.withCredentials,
        context: disablePolling(options.context)
      })
    );
  }
}

function appendParams(url: string, params: URLSearchParams): string {
  const base = url.startsWith('http://') || url.startsWith('https://') ? undefined : 'http://placeholder';
  const target = new URL(url, base);
  for (const [key, value] of params.entries()) {
    target.searchParams.set(key, value);
  }
  if (base) {
    return `${target.pathname}${target.search}${target.hash}`;
  }
  return target.toString();
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

function sleep(ms: number): Promise<void> {
  if (ms <= 0) {
    return Promise.resolve();
  }
  return new Promise(resolve => setTimeout(resolve, ms));
}

function buildError(message: string, status: number, url?: string, body?: unknown): Error {
  const error: any = new Error(body ? `${message} - ${JSON.stringify(body)}` : message);
  error.status = status;
  error.url = url;
  error.body = body;
  return error;
}
