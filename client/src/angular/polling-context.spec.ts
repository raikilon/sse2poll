import { describe, expect, it } from 'vitest';
import {
  DEFAULT_POLLING_OPTIONS,
  POLLING_CONFIG,
  disablePolling,
  normalizePollingOptions,
  withPolling
} from './polling-context';
import { HttpContext } from '@angular/common/http';

describe('polling-context', () => {
  it('withPolling enables polling and merges options', () => {
    const ctx = withPolling({ waitMs: 500, pollIntervalMs: 1000 });
    const cfg = ctx.get(POLLING_CONFIG);

    expect(cfg.enabled).toBe(true);
    expect(cfg.waitMs).toBe(500);
    expect(cfg.pollIntervalMs).toBe(1000);
    expect(cfg.maxPollAttempts).toBeUndefined();
  });

  it('disablePolling flips the flag off while keeping previous values', () => {
    const ctx = withPolling({ waitMs: 250 });
    const disabled = disablePolling(ctx).get(POLLING_CONFIG);

    expect(disabled.enabled).toBe(false);
    expect(disabled.waitMs).toBe(250);
  });

  it('normalizePollingOptions applies defaults and sanitizes values', () => {
    const normalized = normalizePollingOptions({
      waitMs: -5,
      pollIntervalMs: NaN,
      maxPollAttempts: -10
    });

    expect(normalized.waitMs).toBeUndefined();
    expect(normalized.pollIntervalMs).toBe(DEFAULT_POLLING_OPTIONS.pollIntervalMs);
    expect(normalized.maxPollAttempts).toBe(0);
  });

  it('normalizePollingOptions supports null for unlimited attempts', () => {
    const normalized = normalizePollingOptions({ maxPollAttempts: null });
    expect(normalized.maxPollAttempts).toBeNull();
  });

  it('normalizePollingOptions keeps existing values when valid', () => {
    const normalized = normalizePollingOptions({
      waitMs: 1000,
      pollIntervalMs: 750,
      maxPollAttempts: 5
    });

    expect(normalized).toEqual({
      waitMs: 1000,
      pollIntervalMs: 750,
      maxPollAttempts: 5
    });
  });

  it('withPolling defaults to disabled when not set', () => {
    const cfg = new HttpContext().get(POLLING_CONFIG);
    expect(cfg.enabled).toBe(false);
  });
});
