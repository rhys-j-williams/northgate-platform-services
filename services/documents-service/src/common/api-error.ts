import { NextFunction, Request, Response } from 'express';
import { correlation } from './correlation';
import { log } from './logger';

export class ApiError extends Error {
  constructor(public readonly status: number, public readonly code: string, message: string) {
    super(message);
  }
}

export function errorHandler(err: unknown, _req: Request, res: Response, _next: NextFunction): void {
  const e = err instanceof ApiError ? err : new ApiError(500, 'INTERNAL_ERROR', 'unexpected error, quote the correlation id when raising a ticket');
  if (e.status >= 500) {
    log('ERROR', `${e.code}: ${(err as Error).message}`, { stack: (err as Error).stack });
  }
  if (res.headersSent) {
    // mid stream failure; the client gets a truncated PDF and a closed socket. INC0050271.
    res.end();
    return;
  }
  res.status(e.status).json({ code: e.code, message: e.message, status: e.status, correlationId: correlation.current(), timestamp: new Date().toISOString(), violations: [] });
}
