import { Request, Response } from 'express';
import { CorrelationMiddleware } from './correlation.middleware';
import { correlation } from './correlation';

function fakeReq(headers: Record<string, string>): Request {
  return { header: (name: string) => headers[name.toLowerCase()] } as unknown as Request;
}

function fakeRes(): Response & { headers: Record<string, string> } {
  const headers: Record<string, string> = {};
  return { headers, setHeader: (k: string, v: string) => (headers[k] = v) } as unknown as Response & { headers: Record<string, string> };
}

describe('CorrelationMiddleware', () => {
  const mw = new CorrelationMiddleware();

  it('reuses an incoming X-Correlation-Id and echoes it', () => {
    const res = fakeRes();
    let seen = '';
    mw.use(fakeReq({ 'x-correlation-id': 'abc-123' }), res, () => (seen = correlation.current()));
    expect(seen).toBe('abc-123');
    expect(res.headers['X-Correlation-Id']).toBe('abc-123');
  });

  it('generates one when absent', () => {
    const res = fakeRes();
    let seen = '';
    mw.use(fakeReq({}), res, () => (seen = correlation.current()));
    expect(seen).toMatch(/^[0-9a-f-]{36}$/);
    expect(res.headers['X-Correlation-Id']).toBe(seen);
  });

  it('strips characters Bedrock cannot carry and caps the length', () => {
    const res = fakeRes();
    let seen = '';
    mw.use(fakeReq({ 'x-correlation-id': ' bad id;drop table\n' + 'x'.repeat(100) }), res, () => (seen = correlation.current()));
    expect(seen).not.toMatch(/[ ;\n]/);
    expect(seen.length).toBeLessThanOrEqual(64);
  });

  it('reports no-request outside a request scope', () => {
    expect(correlation.current()).toBe('no-request');
  });
});
