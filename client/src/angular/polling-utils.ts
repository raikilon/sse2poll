export function extractJobId(body: unknown): string | undefined {
  if (
    body &&
    typeof body === 'object' &&
    'jobId' in body &&
    typeof (body as any).jobId === 'string'
  ) {
    const id = (body as any).jobId.trim();
    return id.length > 0 ? id : undefined;
  }
  return undefined;
}

export function sleep(ms: number): Promise<void> {
  return ms > 0 ? new Promise(r => setTimeout(r, ms)) : Promise.resolve();
}

export class PollingError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly url?: string,
    public readonly body?: unknown
  ) {
    super(message);
  }
}
