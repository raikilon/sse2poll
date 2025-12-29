import { HttpContext, HttpContextToken } from '@angular/common/http';

export interface PollingOptions {
  waitMs?: number;
  pollIntervalMs?: number;
  maxPollAttempts?: number | null;
}

export interface PollingConfig extends PollingOptions {
  enabled: boolean;
}

export interface NormalizedPollingOptions {
  waitMs?: number;
  pollIntervalMs: number;
  maxPollAttempts: number | null;
}

export const DEFAULT_POLLING_OPTIONS: NormalizedPollingOptions = {
  waitMs: undefined,
  pollIntervalMs: 250,
  maxPollAttempts: 60
} satisfies NormalizedPollingOptions;

export const POLLING_CONFIG = new HttpContextToken<PollingConfig>(() => ({
  enabled: false
}));

export function withPolling(
  options?: Partial<PollingOptions>
): HttpContext {
  return new HttpContext().set(POLLING_CONFIG, {
    enabled: true,
    ...options
  });
}

export function disablePolling(context?: HttpContext): HttpContext {
  const base = context ?? new HttpContext();
  const existing = base.get(POLLING_CONFIG);
  return base.set(POLLING_CONFIG, { ...existing, enabled: false });
}

export function normalizePollingOptions(
  options: PollingOptions
): NormalizedPollingOptions {
  const merged = { ...DEFAULT_POLLING_OPTIONS, ...options };

  const waitMs =
    merged.waitMs === undefined ||
    Number.isNaN(merged.waitMs) ||
    merged.waitMs < 0
      ? undefined
      : Math.max(0, merged.waitMs);

  const pollIntervalMs = Number.isFinite(merged.pollIntervalMs)
    ? Math.max(0, merged.pollIntervalMs)
    : DEFAULT_POLLING_OPTIONS.pollIntervalMs;

  const maxPollAttempts =  merged.maxPollAttempts === null
        ? null
        : Number.isFinite(merged.maxPollAttempts)
          ? Math.max(0, Math.trunc(merged.maxPollAttempts))
          : DEFAULT_POLLING_OPTIONS.maxPollAttempts; 

  return { waitMs, pollIntervalMs, maxPollAttempts };
}
