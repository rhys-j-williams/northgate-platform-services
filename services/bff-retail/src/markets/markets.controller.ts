import { Controller, Get, Query } from '@nestjs/common';
import { AggregioClient, TickerHausClient, TriScoreClient } from '../clients/partner.clients';
import { CacheService } from '../cache/cache.service';
import { CurrentPrincipal } from '../auth/current-principal.decorator';
import { Principal } from '../auth/principal';

/** The "everything else on the dashboard" controller: market ticker, credit score, linked external accounts. */
@Controller()
export class MarketsController {
  constructor(
    private readonly tickerhaus: TickerHausClient,
    private readonly triscore: TriScoreClient,
    private readonly aggregio: AggregioClient,
    private readonly cache: CacheService,
  ) {}

  @Get('markets/quotes')
  quotes(@Query('symbols') symbols?: string) {
    const list = symbols ? symbols.split(',').map((s) => s.trim().toUpperCase()).filter(Boolean) : undefined;
    const key = `quotes:${(list ?? ['default']).join(',')}`;
    return this.cache.getOrLoad(key, () => this.tickerhaus.quotes(list), 60);
  }

  @Get('credit-score')
  creditScore(@CurrentPrincipal() principal: Principal) {
    return this.cache.getOrLoad(`triscore:${principal.customerId}`, () => this.triscore.score(principal.customerId), 6 * 3600);
  }

  @Get('linked-accounts')
  linkedAccounts(@CurrentPrincipal() principal: Principal) {
    return this.cache.getOrLoad(`aggregio:${principal.customerId}`, () => this.aggregio.linkedAccounts(principal.customerId), 300);
  }
}
