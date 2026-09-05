import { CanActivate, ExecutionContext, Injectable, SetMetadata } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Request } from 'express';
import { KeystoneJwtService } from './keystone-jwt.service';
import { ApiException } from '../common/api-error';
import { correlation } from '../common/correlation';
import { PRINCIPAL_KEY } from './principal';

export const PUBLIC_KEY = 'meridian.public';
export const Public = () => SetMetadata(PUBLIC_KEY, true);

@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(private readonly jwt: KeystoneJwtService, private readonly reflector: Reflector) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    if (this.reflector.getAllAndOverride<boolean>(PUBLIC_KEY, [context.getHandler(), context.getClass()])) {
      return true;
    }
    const req = context.switchToHttp().getRequest<Request>();
    const header = req.header('authorization') ?? '';
    if (!header.toLowerCase().startsWith('bearer ')) {
      throw ApiException.unauthorised('TOKEN_MISSING', 'bearer token required');
    }
    const principal = await this.jwt.verify(header.slice(7).trim());
    correlation.bindCustomer(principal.customerId);
    (req as Request & { [PRINCIPAL_KEY]?: unknown })[PRINCIPAL_KEY] = principal;
    return true;
  }
}
