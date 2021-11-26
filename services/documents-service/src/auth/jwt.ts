import { NextFunction, Request, Response } from 'express';
import { createRemoteJWKSet, decodeJwt, jwtVerify, JWTPayload } from 'jose';
import { config } from '../config';
import { ApiError } from '../common/api-error';
import { correlation } from '../common/correlation';
import { log } from '../common/logger';

export interface Principal {
  subject: string;
  customerId: string;
  scopes: string[];
}

declare module 'express-serve-static-core' {
  interface Request {
    principal?: Principal;
    bearer?: string;
  }
}

const jwks = config.authMode === 'jwks' ? createRemoteJWKSet(new URL(config.keystoneJwksUrl), { cooldownDuration: 30_000 }) : undefined;

if (config.authMode === 'insecure-local') {
  log('WARN', 'MERIDIAN_AUTH_MODE=insecure-local: tokens decoded not verified');
}

async function verify(token: string): Promise<JWTPayload> {
  if (!jwks) {
    const p = decodeJwt(token);
    if (p.exp !== undefined && p.exp * 1000 < Date.now()) {
      throw new Error('token expired');
    }
    return p;
  }
  const { payload } = await jwtVerify(token, jwks, { issuer: config.keystoneIssuer, audience: config.keystoneAudiences, clockTolerance: 30 });
  return payload;
}

export function toPrincipal(payload: JWTPayload): Principal {
  const customerId = (payload['customer_id'] ?? payload['cid'] ?? payload.sub) as string | undefined;
  if (!payload.sub || !customerId) {
    throw new ApiError(401, 'TOKEN_CLAIMS', 'token has no sub or customer_id claim');
  }
  const scope = payload['scope'];
  return { subject: payload.sub, customerId, scopes: typeof scope === 'string' ? scope.split(' ') : [] };
}

export function requireJwt(req: Request, _res: Response, next: NextFunction): void {
  const header = req.header('authorization') ?? '';
  if (!header.toLowerCase().startsWith('bearer ')) {
    next(new ApiError(401, 'TOKEN_MISSING', 'bearer token required'));
    return;
  }
  const token = header.slice(7).trim();
  verify(token)
    .then((payload) => {
      req.principal = toPrincipal(payload);
      req.bearer = token;
      correlation.bindCustomer(req.principal.customerId);
      next();
    })
    .catch((err: Error) => next(err instanceof ApiError ? err : new ApiError(401, 'TOKEN_INVALID', `token rejected: ${err.message}`)));
}
