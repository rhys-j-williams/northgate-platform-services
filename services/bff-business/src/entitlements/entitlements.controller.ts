import { Controller, Get } from '@nestjs/common';
import { EntitlementsService } from './entitlements.service';
import { EntitlementsClient } from '../clients/entitlements.client';
import { CurrentPrincipal } from '../auth/current-principal.decorator';
import { Principal } from '../auth/principal';

@Controller()
export class EntitlementsController {
  constructor(private readonly entitlements: EntitlementsService, private readonly client: EntitlementsClient) {}

  @Get('me')
  me(@CurrentPrincipal() principal: Principal) {
    return this.entitlements.effective(principal);
  }

  @Get('organisation/users')
  async users(@CurrentPrincipal() principal: Principal) {
    await this.entitlements.require(principal, 'users:manage');
    return this.client.forOrganisation(principal.customerId);
  }
}
