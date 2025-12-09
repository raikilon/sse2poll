import { HttpContext, HttpContextToken } from '@angular/common/http';

export interface PollingConfig {
  enabled: boolean;
  waitMs?: number;
  pollIntervalMs?: number;
  maxPollAttempts?: number | null;
}

export const POLLING_CONFIG = new HttpContextToken<PollingConfig>(() => ({ enabled: false }));

export function withPolling(options?: Partial<Omit<PollingConfig, 'enabled'>>): HttpContext {
  return new HttpContext().set(POLLING_CONFIG, { enabled: true, ...options });
}

export function disablePolling(context?: HttpContext): HttpContext {
  const base = context ?? new HttpContext();
  return base.set(POLLING_CONFIG, { enabled: false });
}
