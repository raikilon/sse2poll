import {
  HttpClient,
  HttpContext,
  HttpHeaders,
  HttpResponse,
  HttpParams
} from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { disablePolling } from './polling-context';
import { extractJobId, sleep, PollingError } from './polling-utils';

export interface PollingOrchestratorOptions {
  url: string;
  jobId: string;
  waitMs?: number;
  pollIntervalMs: number;
  maxPollAttempts: number | null;
  headers?: HttpHeaders;
  withCredentials?: boolean;
  context?: HttpContext;
  signal?: AbortSignal;
}

export interface PollingResult<T> {
  jobId: string;
  attempt: number;
  payload: T;
}

export class PollingOrchestrator {
  constructor(private readonly http: HttpClient) {}

  async pollUntilReady<T>(
    options: PollingOrchestratorOptions
  ): Promise<PollingResult<T>> {
    let attempt = 0;
    let jobId = options.jobId;

    const maxAttempts = options.maxPollAttempts;

    while (maxAttempts === null || attempt < maxAttempts) {
      if (options.signal?.aborted) {
        throw new PollingError('Polling aborted', 499, options.url);
      }

      const response = await this.issuePoll<T>(options, jobId);

      if (response.status === 200) {
        return {
          jobId,
          attempt,
          payload: response.body as T
        };
      }

      if (response.status === 202) {
        jobId = extractJobId(response.body) ?? jobId;
        attempt++;
        await sleep(options.pollIntervalMs);
        continue;
      }

      if (response.status === 404) {
        throw new PollingError(
          `Job ${jobId} not found`,
          404,
          options.url
        );
      }

      throw new PollingError(
        'Polling failed',
        response.status,
        options.url,
        response.body
      );
    }

    throw new PollingError(
      `Polling exceeded ${options.maxPollAttempts} attempts`,
      504,
      options.url
    );
  }

  private issuePoll<T>(
    options: PollingOrchestratorOptions,
    jobId: string
  ): Promise<HttpResponse<T>> {
    let params = new HttpParams().set('job', jobId);

    if (options.waitMs !== undefined) {
      params = params.set('waitMs', String(options.waitMs));
    }

    return firstValueFrom(
      this.http.get<T>(options.url, {
        observe: 'response',
        responseType: 'json',
        params,
        headers: options.headers,
        withCredentials: options.withCredentials,
        context: disablePolling(options.context)
      })
    );
  }
}
