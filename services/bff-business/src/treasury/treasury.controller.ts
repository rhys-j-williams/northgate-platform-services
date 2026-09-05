import { Controller, Get } from '@nestjs/common';
import { BedrockClient } from '../clients/bedrock.client';
import { ExposureClient, Position } from '../clients/exposure.client';
import { EntitlementsService } from '../entitlements/entitlements.service';
import { CurrentPrincipal } from '../auth/current-principal.decorator';
import { Principal } from '../auth/principal';

/** Ledgerline positions page. Buckets are a BFF concern; exposure-calc only sees numbers. */
@Controller('treasury')
export class TreasuryController {
  constructor(private readonly bedrock: BedrockClient, private readonly exposure: ExposureClient, private readonly entitlements: EntitlementsService) {}

  @Get('positions')
  async positions(@CurrentPrincipal() principal: Principal) {
    await this.entitlements.require(principal, 'positions:view');
    const accounts = await this.bedrock.accountsForCustomer(principal.customerId);
    const positions: Position[] = accounts.map((a) => ({
      accountId: a.accountId,
      currency: 'USD',
      balanceMinor: a.currentBalanceMinor,
      bucket: a.type === 'TREASURY-OPERATING' || a.type === 'BUSINESS-CHECKING' ? 'operating' : a.currentBalanceMinor < 0 ? 'debt' : 'reserve',
    }));
    const report = await this.exposure.exposure(principal.customerId, positions);
    return { positions, report };
  }
}
