import { Injectable } from '@nestjs/common';
import { UpstreamClient } from './upstream-client';
import { FixturesService } from './fixtures.service';
import { config } from '../config';
import { ApiException } from '../common/api-error';

/** Wire shape from bedrock-adapter /bedrock/v1. Amounts in minor units, dates ISO. */
export interface BedrockAccount {
  accountId: string;
  customerId: string;
  type: string;
  accountNumber: string;
  routingNumber: string;
  currentBalanceMinor: number;
  availableBalanceMinor: number;
  openedDate: string;
  status: string;
  ownerName: string;
}

export interface BedrockTransaction {
  transactionId: string;
  accountId: string;
  postedDate: string;
  settledDate: string | null;
  amountMinor: number;
  runningBalanceMinor: number;
  mcc: string;
  channel: string;
  status: string;
  description: string;
}

@Injectable()
export class BedrockClient extends UpstreamClient {
  constructor(private readonly fixtures: FixturesService) {
    super('bedrock-adapter', config.bedrockAdapterUrl);
  }

  accountsForCustomer(customerId: string): Promise<BedrockAccount[]> {
    return this.callOrFallback(
      async () => (await this.http.get<BedrockAccount[]>(`/customers/${customerId}/accounts`)).data,
      () =>
        this.fixtures
          .get()
          .accounts.filter((a) => a.customerId === customerId)
          .map((a) => this.fixtureAccount(a.accountId)),
    );
  }

  account(accountId: string): Promise<BedrockAccount> {
    return this.callOrFallback(
      async () => (await this.http.get<BedrockAccount>(`/accounts/${accountId}`)).data,
      () => this.fixtureAccount(accountId),
    );
  }

  transactions(accountId: string, limit: number): Promise<BedrockTransaction[]> {
    return this.callOrFallback(
      async () => (await this.http.get<BedrockTransaction[]>(`/accounts/${accountId}/transactions`, { params: { limit } })).data,
      () =>
        this.fixtures
          .get()
          .transactions.filter((t) => t.accountId === accountId)
          .sort((a, b) => b.postedAt.localeCompare(a.postedAt))
          .slice(0, limit)
          .map((t) => ({
            transactionId: t.transactionId,
            accountId: t.accountId,
            postedDate: t.postedAt.slice(0, 10),
            settledDate: t.settledAt ? t.settledAt.slice(0, 10) : null,
            amountMinor: t.amountMinor,
            runningBalanceMinor: t.runningBalanceMinor,
            mcc: t.merchantCategoryCode,
            channel: t.channel.toUpperCase(),
            status: t.status.toUpperCase(),
            description: t.description,
          })),
    );
  }

  private fixtureAccount(accountId: string): BedrockAccount {
    const set = this.fixtures.get();
    const a = set.accounts.find((x) => x.accountId === accountId);
    if (!a) {
      throw ApiException.notFound('BEDROCK_NOT_FOUND', `account ${accountId} not found`);
    }
    const c = set.customers.find((x) => x.customerId === a.customerId);
    return {
      accountId: a.accountId,
      customerId: a.customerId,
      type: a.type.toUpperCase(),
      accountNumber: a.accountNumber,
      routingNumber: a.routingNumber,
      currentBalanceMinor: a.currentBalanceMinor,
      availableBalanceMinor: a.availableBalanceMinor,
      openedDate: a.openedAt.slice(0, 10),
      status: a.status.toUpperCase(),
      ownerName: c?.displayName ?? '',
    };
  }
}
