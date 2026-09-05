import { AsyncLocalStorage } from 'async_hooks';
import { randomUUID } from 'crypto';

export const CORRELATION_HEADER = 'x-correlation-id';

interface RequestContext {
  correlationId: string;
  customerId?: string;
  /** Caller's bearer token, relayed to the Java resource servers as-is (PLAT-1044 token relay). */
  bearerToken?: string;
}

const storage = new AsyncLocalStorage<RequestContext>();

export const correlation = {
  header: CORRELATION_HEADER,
  run<T>(ctx: RequestContext, fn: () => T): T {
    return storage.run(ctx, fn);
  },
  current(): string {
    return storage.getStore()?.correlationId ?? 'no-request';
  },
  customerId(): string | undefined {
    return storage.getStore()?.customerId;
  },
  bindCustomer(customerId: string, bearerToken?: string): void {
    const store = storage.getStore();
    if (store) {
      store.customerId = customerId;
      store.bearerToken = bearerToken;
    }
  },
  bearerToken(): string | undefined {
    return storage.getStore()?.bearerToken;
  },
  generate(): string {
    return randomUUID();
  },
  /** Bedrock's MTAI header only carries 32 characters; the adapter truncates too, keep them equal. */
  sanitise(value: string | undefined): string | undefined {
    if (!value) {
      return undefined;
    }
    const trimmed = value.trim().replace(/[^A-Za-z0-9-]/g, '');
    return trimmed.length === 0 ? undefined : trimmed.slice(0, 64);
  },
};
