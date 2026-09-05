import { BadRequestException, HttpStatus } from '@nestjs/common';
import { ApiExceptionFilter } from './api-exception.filter';
import { ApiException } from './api-error';

describe('ApiExceptionFilter', () => {
  const filter = new ApiExceptionFilter();

  it('maps ApiException to the shared error body', () => {
    const body = filter.toBody(ApiException.conflict('INSUFFICIENT_FUNDS', 'no'));
    expect(body).toMatchObject({ code: 'INSUFFICIENT_FUNDS', status: 409, message: 'no', violations: [] });
    expect(body.timestamp).toMatch(/^\d{4}-/);
  });

  it('turns class-validator output into violations', () => {
    const body = filter.toBody(new BadRequestException(['amountMinor must be a positive number', 'fromAccountId must match']));
    expect(body.code).toBe('VALIDATION_FAILED');
    expect(body.violations.map((v) => v.field)).toEqual(['amountMinor', 'fromAccountId']);
  });

  it('never leaks unexpected exception messages', () => {
    const body = filter.toBody(new Error('ORA-00942 table or view does not exist'));
    expect(body.status).toBe(HttpStatus.INTERNAL_SERVER_ERROR);
    expect(body.message).not.toContain('ORA-');
    expect(body.code).toBe('INTERNAL_ERROR');
  });
});
