import { AsyncLocalStorage } from 'async_hooks';
import { randomUUID } from 'crypto';
import { NextFunction, Request, Response } from 'express';

interface Ctx {
  correlationId: string;
  customerId?: string;
}

const storage = new AsyncLocalStorage<Ctx>();

export const correlation = {
  current: (): string => storage.getStore()?.correlationId ?? 'no-request',
  customerId: (): string | undefined => storage.getStore()?.customerId,
  bindCustomer(id: string): void {
    const s = storage.getStore();
    if (s) {
      s.customerId = id;
    }
  },
};

export function correlationMiddleware(req: Request, res: Response, next: NextFunction): void {
  const raw = (req.header('x-correlation-id') ?? '').trim().replace(/[^A-Za-z0-9-]/g, '').slice(0, 64);
  const id = raw.length > 0 ? raw : randomUUID();
  res.setHeader('X-Correlation-Id', id);
  storage.run({ correlationId: id }, () => next());
}
