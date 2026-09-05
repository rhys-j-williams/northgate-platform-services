import { Injectable, NestMiddleware } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';
import { correlation } from './correlation';

@Injectable()
export class CorrelationMiddleware implements NestMiddleware {
  use = (req: Request, res: Response, next: NextFunction): void => {
    const incoming = correlation.sanitise(req.header(correlation.header));
    const id = incoming ?? correlation.generate();
    res.setHeader('X-Correlation-Id', id);
    correlation.run({ correlationId: id }, () => next());
  };
}
