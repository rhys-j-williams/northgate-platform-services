import { Injectable, NestMiddleware } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';
import { correlation } from './correlation';
import { StructuredLogger } from './logger';

@Injectable()
export class CorrelationMiddleware implements NestMiddleware {
  private readonly access = new StructuredLogger('http.access');

  use = (req: Request, res: Response, next: NextFunction): void => {
    const incoming = correlation.sanitise(req.header(correlation.header));
    const id = incoming ?? correlation.generate();
    res.setHeader('X-Correlation-Id', id);
    const started = Date.now();
    correlation.run({ correlationId: id }, () => {
      if (typeof res.on === 'function' && !(req.originalUrl ?? '').startsWith('/health')) {
        res.on('finish', () =>
          this.access.log(`http.request method=${req.method} path=${req.originalUrl} status=${res.statusCode} durationMs=${Date.now() - started}`),
        );
      }
      next();
    });
  };
}
