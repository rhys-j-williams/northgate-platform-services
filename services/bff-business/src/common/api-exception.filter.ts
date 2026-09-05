import { ArgumentsHost, Catch, ExceptionFilter, HttpException, HttpStatus } from '@nestjs/common';
import { Response } from 'express';
import { ApiErrorBody, ApiException } from './api-error';
import { correlation } from './correlation';
import { StructuredLogger } from './logger';

@Catch()
export class ApiExceptionFilter implements ExceptionFilter {
  private readonly logger = new StructuredLogger('ApiExceptionFilter');

  catch(exception: unknown, host: ArgumentsHost): void {
    const res = host.switchToHttp().getResponse<Response>();
    const body = this.toBody(exception);
    if (body.status >= 500) {
      this.logger.error(`${body.code}: ${body.message}`, exception instanceof Error ? exception.stack : undefined);
    }
    res.status(body.status).json(body);
  }

  toBody(exception: unknown): ApiErrorBody {
    const base = { correlationId: correlation.current(), timestamp: new Date().toISOString() };
    if (exception instanceof ApiException) {
      return { ...base, code: exception.code, message: exception.message, status: exception.getStatus(), violations: exception.violations };
    }
    if (exception instanceof HttpException) {
      const status = exception.getStatus();
      const response = exception.getResponse();
      const violations: ApiErrorBody['violations'] = [];
      let message = exception.message;
      if (typeof response === 'object' && response !== null && Array.isArray((response as { message?: unknown }).message)) {
        // class-validator output: one string per violation
        for (const v of (response as { message: string[] }).message) {
          violations.push({ field: v.split(' ')[0], message: v });
        }
        message = 'request validation failed';
      }
      return { ...base, code: status === 400 ? 'VALIDATION_FAILED' : `HTTP_${status}`, message, status, violations };
    }
    return {
      ...base,
      code: 'INTERNAL_ERROR',
      message: 'unexpected error, quote the correlation id when raising a ticket',
      status: HttpStatus.INTERNAL_SERVER_ERROR,
      violations: [],
    };
  }
}
