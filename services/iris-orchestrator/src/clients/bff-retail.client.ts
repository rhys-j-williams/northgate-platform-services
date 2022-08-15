import { Injectable } from '@nestjs/common';
import { upstream } from '../common/http';
import { config } from '../config';
import { StructuredLogger } from '../common/logger';
import { FixturesService } from '../fixtures.service';

export interface BalanceLine {
  accountId: string;
  nickname: string;
  maskedNumber: string;
  available: string;
}

export interface TxnLine {
  postedOn: string;
  description: string;
  amount: string;
}

/**
 * The only upstream. We forward the widget's bearer token untouched; bff-retail does the
 * authorisation and the masking. If it is down we fall back to fixtures locally so the widget demo
 * still works (the Iris squad demos more than anyone).
 */
@Injectable()
export class BffRetailClient {
  private readonly logger = new StructuredLogger('BffRetailClient');
  private readonly http = upstream(config.bffRetailUrl);

  constructor(private readonly fixtures: FixturesService) {}

  async balances(bearer: string, customerId: string): Promise<BalanceLine[]> {
    try {
      const res = await this.http.get<Array<{ accountId: string; nickname: string; maskedNumber: string; availableBalance: { amount: string } }>>(
        '/accounts',
        { headers: { Authorization: `Bearer ${bearer}` } },
      );
      return res.data.map((a) => ({ accountId: a.accountId, nickname: a.nickname, maskedNumber: a.maskedNumber, available: a.availableBalance.amount }));
    } catch (err) {
      return this.fallback('balances', err, () =>
        this.fixtures
          .get()
          .accounts.filter((a) => a.customerId === customerId)
          .map((a) => ({
            accountId: a.accountId,
            nickname: a.nickname,
            maskedNumber: `****${a.accountNumber.slice(-4)}`,
            available: (a.availableBalanceMinor / 100).toFixed(2),
          })),
      );
    }
  }

  async recentTransactions(bearer: string, customerId: string, last4?: string, limit = 5): Promise<TxnLine[]> {
    try {
      const accounts = await this.balances(bearer, customerId);
      const account = last4 ? accounts.find((a) => a.maskedNumber.endsWith(last4)) : accounts[0];
      if (!account) {
        return [];
      }
      const res = await this.http.get<Array<{ postedOn: string; description: string; amount: { amount: string } }>>(
        `/accounts/${account.accountId}/transactions`,
        { params: { limit }, headers: { Authorization: `Bearer ${bearer}` } },
      );
      return res.data.map((t) => ({ postedOn: t.postedOn, description: t.description, amount: t.amount.amount }));
    } catch (err) {
      return this.fallback('transactions', err, () => {
        const fx = this.fixtures.get();
        const accts = fx.accounts.filter((a) => a.customerId === customerId);
        const acct = last4 ? accts.find((a) => a.accountNumber.endsWith(last4)) : accts[0];
        if (!acct) {
          return [];
        }
        return fx.transactions
          .filter((t) => t.accountId === acct.accountId)
          .sort((a, b) => (a.postedAt < b.postedAt ? 1 : -1))
          .slice(0, limit)
          .map((t) => ({ postedOn: t.postedAt.slice(0, 10), description: t.description, amount: (t.amountMinor / 100).toFixed(2) }));
      });
    }
  }

  private fallback<T>(what: string, err: unknown, loader: () => T): T {
    if (!config.fixtureFallback) {
      throw err;
    }
    this.logger.warn(`bff-retail unavailable for ${what}, answering from fixtures: ${(err as Error).message}`);
    return loader();
  }
}
