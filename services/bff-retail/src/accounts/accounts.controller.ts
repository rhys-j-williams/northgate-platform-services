import { Controller, DefaultValuePipe, Get, Param, ParseIntPipe, Query } from '@nestjs/common';
import { AccountsService } from './accounts.service';
import { CurrentPrincipal } from '../auth/current-principal.decorator';
import { Principal } from '../auth/principal';

@Controller()
export class AccountsController {
  constructor(private readonly accounts: AccountsService) {}

  @Get('dashboard')
  dashboard(@CurrentPrincipal() principal: Principal) {
    return this.accounts.dashboard(principal);
  }

  @Get('accounts')
  list(@CurrentPrincipal() principal: Principal) {
    return this.accounts.list(principal);
  }

  @Get('accounts/:accountId')
  get(@CurrentPrincipal() principal: Principal, @Param('accountId') accountId: string) {
    return this.accounts.get(principal, accountId);
  }

  @Get('accounts/:accountId/transactions')
  transactions(
    @CurrentPrincipal() principal: Principal,
    @Param('accountId') accountId: string,
    @Query('limit', new DefaultValuePipe(50), ParseIntPipe) limit: number,
  ) {
    return this.accounts.transactions(principal, accountId, limit);
  }
}
