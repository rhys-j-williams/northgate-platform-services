import { Injectable } from '@nestjs/common';
import { createRemoteJWKSet, decodeJwt, jwtVerify, JWTPayload } from 'jose';
import { config } from '../config';
import { ApiException } from '../common/api-error';
import { StructuredLogger } from '../common/logger';
import { Principal } from './principal';

type Verifier = (token: string) => Promise<JWTPayload>;

/**
 * Validates Keystone access tokens against the JWKS endpoint. Keys are cached by jose for the
 * cooldown period; a kid we have not seen triggers one refetch. Keystone rotates keys quarterly
 * (KEY-1650) and the last rotation in March 2025 caused INC0051192 because the old cache here
 * did not refetch on unknown kid. Do not reintroduce a hand rolled cache.
 */
@Injectable()
export class KeystoneJwtService {
  private readonly logger = new StructuredLogger('KeystoneJwtService');
  private verifier: Verifier;

  constructor() {
    this.verifier = config.authMode === 'insecure-local' ? this.insecureVerifier() : this.jwksVerifier();
    if (config.authMode === 'insecure-local') {
      this.logger.warn('MERIDIAN_AUTH_MODE=insecure-local: tokens are decoded, not verified. Local use only.');
    }
  }

  async verify(token: string): Promise<Principal> {
    let payload: JWTPayload;
    try {
      payload = await this.verifier(token);
    } catch (err) {
      throw ApiException.unauthorised('TOKEN_INVALID', `token rejected: ${(err as Error).message}`);
    }
    return this.toPrincipal(payload);
  }

  toPrincipal(payload: JWTPayload): Principal {
    const customerId = (payload['customer_id'] ?? payload['cid']) as string | undefined;
    if (!payload.sub || !customerId) {
      throw ApiException.unauthorised('TOKEN_CLAIMS', 'token has no sub or customer_id claim');
    }
    const scope = payload['scope'];
    const scopes = typeof scope === 'string' ? scope.split(' ') : Array.isArray(scope) ? (scope as string[]) : [];
    return {
      subject: payload.sub,
      customerId,
      segment: ((payload['segment'] as string | undefined) ?? 'consumer') as Principal['segment'],
      scopes,
      mfaAt: typeof payload['mfa_at'] === 'number' ? (payload['mfa_at'] as number) : undefined,
      sessionId: payload['sid'] as string | undefined,
    };
  }

  private jwksVerifier(): Verifier {
    const jwks = createRemoteJWKSet(new URL(config.keystoneJwksUrl), { cooldownDuration: 30_000 });
    return async (token) => {
      const { payload } = await jwtVerify(token, jwks, {
        issuer: config.keystoneIssuer,
        audience: config.keystoneAudience,
        clockTolerance: 30,
      });
      return payload;
    };
  }

  private insecureVerifier(): Verifier {
    return async (token) => {
      const payload = decodeJwt(token);
      if (payload.exp !== undefined && payload.exp * 1000 < Date.now()) {
        throw new Error('token expired');
      }
      return payload;
    };
  }
}
