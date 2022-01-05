import { Controller, Get } from '@nestjs/common';
import { maskAccountNumber } from '@meridian/domain-fixtures';
import { BedrockClient } from '../clients/bedrock.client';
import { CacheService } from '../cache/cache.service';
import { EntitlementsService } from '../entitlements/entitlements.service';
import { CurrentPrincipal } from '../auth/current-principal.decorator';
import { Principal } from '../auth/principal';

@Controller('accounts')
export class AccountsController {
  constructor(private readonly bedrock: BedrockClient, private readonly cache: CacheService, private readonly entitlements: EntitlementsService) {}

  @Get()
  async list(@CurrentPrincipal() principal: Principal) {
    await this.entitlements.require(principal, 'accounts:view');
    const raw = await this.cache.getOrLoad(`accounts:${principal.customerId}`, () => this.bedrock.accountsForCustomer(principal.customerId));
    return raw.map((a) => ({
      accountId: a.accountId,
      type: a.type,
      maskedNumber: maskAccountNumber(a.accountNumber),
      ownerName: a.ownerName,
      currentBalanceMinor: a.currentBalanceMinor,
      availableBalanceMinor: a.availableBalanceMinor,
      status: a.status,
    }));
  }
}
