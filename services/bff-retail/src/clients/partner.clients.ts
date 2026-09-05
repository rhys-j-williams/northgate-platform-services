import { Injectable } from '@nestjs/common';
import { UpstreamClient } from './upstream-client';
import { FixturesService } from './fixtures.service';
import { config } from '../config';

/*
 * The four partner integrations. Each one has its own vendor contract and its own mock in
 * mock-external; the shapes below are the subset retail-web renders. TickerHaus market data is the
 * only one that is not customer specific and therefore the only one cached across customers.
 */

export interface LinkedAccount {
  provider: string;
  institutionLabel: string;
  accountLabel: string;
  lastFour: string;
  balanceMinor: number;
  asOf: string;
}

export interface Quote {
  symbol: string;
  price: number;
  changePct: number;
  asOf: string;
}

export interface CreditScore {
  score: number;
  band: 'poor' | 'fair' | 'good' | 'very-good' | 'exceptional';
  asOf: string;
  factors: string[];
}

export interface PayLinkContact {
  contactId: string;
  displayName: string;
  handle: string;
  verified: boolean;
}

export interface PayLinkPayment {
  paymentId: string;
  contactId: string;
  amountMinor: number;
  memo?: string;
  status: 'pending' | 'sent' | 'requested' | 'failed';
  createdAt: string;
}

@Injectable()
export class AggregioClient extends UpstreamClient {
  constructor(private readonly fixtures: FixturesService) {
    super('aggregio', config.aggregioUrl);
  }

  linkedAccounts(customerId: string): Promise<LinkedAccount[]> {
    return this.callOrFallback(
      async () => (await this.http.get<LinkedAccount[]>(`/v2/customers/${customerId}/linked-accounts`)).data,
      () => {
        // one synthetic external account per external-transfer payee, balance derived from the payee id
        const payees = this.fixtures.get().payees.filter((p) => p.customerId === customerId && p.type === 'external-transfer');
        return payees.map((p) => ({
          provider: 'aggregio',
          institutionLabel: p.name,
          accountLabel: p.nickname,
          lastFour: p.accountNumberLastFour,
          balanceMinor: (parseInt(p.payeeId.replace(/\D/g, '').slice(-5) || '0', 10) % 900000) + 12500,
          asOf: new Date().toISOString(),
        }));
      },
    );
  }
}

@Injectable()
export class TickerHausClient extends UpstreamClient {
  private static readonly WATCHLIST = ['MTBK', 'SPX', 'NDQ', 'US10Y'];

  constructor() {
    super('tickerhaus', config.tickerhausUrl);
  }

  quotes(symbols: string[] = TickerHausClient.WATCHLIST): Promise<Quote[]> {
    return this.callOrFallback(
      async () => (await this.http.get<Quote[]>('/quotes', { params: { symbols: symbols.join(',') } })).data,
      () =>
        symbols.map((symbol, i) => ({
          symbol,
          price: [42.17, 5211.4, 18244.9, 4.31][i % 4],
          changePct: [0.42, -0.18, 0.07, 0.02][i % 4],
          asOf: '2024-11-15T21:00:00Z',
        })),
    );
  }
}

@Injectable()
export class TriScoreClient extends UpstreamClient {
  constructor() {
    super('triscore', config.triscoreUrl);
  }

  score(customerId: string): Promise<CreditScore> {
    return this.callOrFallback(
      async () => (await this.http.get<CreditScore>(`/v1/consumers/${customerId}/score`)).data,
      () => {
        const digits = parseInt(customerId.replace(/\D/g, '') || '0', 10);
        const score = 640 + (digits % 180);
        return {
          score,
          band: score >= 800 ? 'exceptional' : score >= 740 ? 'very-good' : score >= 670 ? 'good' : 'fair',
          asOf: '2024-11-01',
          factors: ['Length of credit history', 'Credit utilisation'],
        };
      },
    );
  }

  /** Payee verification stub used by bill pay add-payee. Always "verified" in the fixture path. */
  verifyPayee(name: string, routingNumber: string): Promise<{ verified: boolean; reference: string }> {
    return this.callOrFallback(
      async () => (await this.http.post<{ verified: boolean; reference: string }>('/v1/payees/verify', { name, routingNumber })).data,
      () => ({ verified: routingNumber === '021000000', reference: `TRI-FIX-${name.length}` }),
    );
  }
}

@Injectable()
export class PayLinkClient extends UpstreamClient {
  private readonly memory = new Map<string, PayLinkPayment[]>();

  constructor(private readonly fixtures: FixturesService) {
    super('paylink', config.paylinkUrl);
  }

  contacts(customerId: string): Promise<PayLinkContact[]> {
    return this.callOrFallback(
      async () => (await this.http.get<PayLinkContact[]>(`/v1/members/${customerId}/contacts`)).data,
      () =>
        this.fixtures
          .get()
          .payees.filter((p) => p.customerId === customerId && p.type === 'paylink')
          .map((p) => ({ contactId: p.payeeId, displayName: p.name, handle: `${p.nickname.toLowerCase().replace(/\s+/g, '.')}@example.com`, verified: p.verified })),
    );
  }

  send(customerId: string, contactId: string, amountMinor: number, memo: string | undefined, request: boolean): Promise<PayLinkPayment> {
    return this.callOrFallback(
      async () =>
        (await this.http.post<PayLinkPayment>(`/v1/members/${customerId}/payments`, { contactId, amountMinor, memo, direction: request ? 'request' : 'send' })).data,
      () => {
        const payment: PayLinkPayment = {
          paymentId: `PLK-${Date.now().toString(36).toUpperCase()}`,
          contactId,
          amountMinor,
          memo,
          status: request ? 'requested' : 'sent',
          createdAt: new Date().toISOString(),
        };
        const list = this.memory.get(customerId) ?? [];
        list.unshift(payment);
        this.memory.set(customerId, list);
        return payment;
      },
    );
  }

  activity(customerId: string): Promise<PayLinkPayment[]> {
    return this.callOrFallback(
      async () => (await this.http.get<PayLinkPayment[]>(`/v1/members/${customerId}/payments`)).data,
      () => this.memory.get(customerId) ?? [],
    );
  }
}
