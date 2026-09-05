import { KeystoneJwtService } from './keystone-jwt.service';
import { testToken } from '../../test/token';

describe('KeystoneJwtService', () => {
  const svc = new KeystoneJwtService();

  it('builds a principal from Keystone claims', async () => {
    const p = await svc.verify(testToken({ sub: 'u-1', customer_id: 'CUS-100000', segment: 'consumer', scope: 'accounts:read transfers:write', mfa_at: 1700000000 }));
    expect(p).toEqual({ subject: 'u-1', customerId: 'CUS-100000', segment: 'consumer', scopes: ['accounts:read', 'transfers:write'], mfaAt: 1700000000, sessionId: undefined });
  });

  it('rejects tokens without a customer claim', async () => {
    await expect(svc.verify(testToken({ sub: 'u-1' }))).rejects.toMatchObject({ code: 'TOKEN_CLAIMS' });
  });

  it('rejects expired tokens even in insecure-local mode', async () => {
    await expect(svc.verify(testToken({ sub: 'u-1', customer_id: 'CUS-1', exp: 1 }))).rejects.toMatchObject({ code: 'TOKEN_INVALID' });
  });

  // TODO PLAT-2140: no test of the real JWKS path. Needs the keystone-idp-mock running or a
  // local JWKS server in the suite; parked since the March 2025 rotation incident.
});
