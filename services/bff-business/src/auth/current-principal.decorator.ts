import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import { Request } from 'express';
import { Principal, PRINCIPAL_KEY } from './principal';

export const CurrentPrincipal = createParamDecorator((_data: unknown, ctx: ExecutionContext): Principal => {
  const req = ctx.switchToHttp().getRequest<Request & { [PRINCIPAL_KEY]?: Principal }>();
  const principal = req[PRINCIPAL_KEY];
  if (!principal) {
    throw new Error('CurrentPrincipal used on a route without JwtAuthGuard');
  }
  return principal;
});
