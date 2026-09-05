import { Injectable } from '@nestjs/common';
import { maskAccountNumber } from '@meridian/domain-fixtures';
import { BedrockAccount, BedrockClient, BedrockTransaction } from '../clients/bedrock.client';
import { CacheService } from '../cache/cache.service';
import { FixturesService } from '../clients/fixtures.service';
import { ApiException } from '../common/api-error';
import { AccountSummaryDto, DashboardDto, TransactionDto } from './account.dto';
import { categoryForMcc, money } from './money';
import { Principal } from '../auth/principal';

const BORROWING_TYPES = new Set(['CREDIT-CARD', 'MORTGAGE', 'AUTO-LOAN']);

@Injectable()
export class AccountsService {
  constructor(private readonly bedrock: BedrockClient, private readonly cache: CacheService, private readonly fixtures: FixturesService) {}

  async dashboard(principal: Principal): Promise<DashboardDto> {
    const accounts = await this.list(principal);
    const deposits = accounts.filter((a) => !BORROWING_TYPES.has(a.type)).reduce((s, a) => s + a.currentBalance.minor, 0);
    const borrowing = accounts.filter((a) => BORROWING_TYPES.has(a.type)).reduce((s, a) => s + a.currentBalance.minor, 0);
    const customer = this.fixtures.get().customers.find((c) => c.customerId === principal.customerId);
    return {
      customerId: principal.customerId,
      displayName: customer?.displayName ?? principal.subject,
      accounts,
      totals: { deposits: money(deposits), borrowing: money(borrowing) },
      asOf: new Date().toISOString(),
    };
  }

  async list(principal: Principal): Promise<AccountSummaryDto[]> {
    const raw = await this.cache.getOrLoad(`accounts:${principal.customerId}`, () => this.bedrock.accountsForCustomer(principal.customerId));
    return raw.map((a) => this.toSummary(a));
  }

  async get(principal: Principal, accountId: string): Promise<AccountSummaryDto> {
    const raw = await this.cache.getOrLoad(`account:${accountId}`, () => this.bedrock.account(accountId));
    this.assertOwner(principal, raw);
    return this.toSummary(raw);
  }

  async transactions(principal: Principal, accountId: string, limit = 50): Promise<TransactionDto[]> {
    await this.get(principal, accountId);
    const bounded = Math.min(Math.max(limit, 1), 200);
    const raw = await this.bedrock.transactions(accountId, bounded);
    return raw.map((t) => this.toTransaction(t));
  }

  toSummary(a: BedrockAccount): AccountSummaryDto {
    const nickname = this.fixtures.get().accounts.find((f) => f.accountId === a.accountId)?.nickname ?? this.defaultNickname(a.type);
    return {
      accountId: a.accountId,
      type: a.type,
      nickname,
      maskedNumber: maskAccountNumber(a.accountNumber),
      currentBalance: money(a.currentBalanceMinor),
      availableBalance: money(a.availableBalanceMinor),
      status: a.status,
      openedOn: a.openedDate,
    };
  }

  toTransaction(t: BedrockTransaction): TransactionDto {
    return {
      transactionId: t.transactionId,
      accountId: t.accountId,
      postedOn: t.postedDate,
      settledOn: t.settledDate,
      description: t.description,
      merchantCategoryCode: t.mcc,
      category: categoryForMcc(t.mcc),
      amount: money(t.amountMinor),
      runningBalance: money(t.runningBalanceMinor),
      status: t.status,
      channel: t.channel,
      pending: t.status === 'PENDING' || t.settledDate === null,
    };
  }

  private assertOwner(principal: Principal, account: BedrockAccount): void {
    if (account.customerId !== principal.customerId && !principal.scopes.includes('accounts:any')) {
      // 404 not 403: do not confirm the account exists to someone who does not own it (GIS-4471)
      throw ApiException.notFound('ACCOUNT_NOT_FOUND', `account ${account.accountId} not found`);
    }
  }

  private defaultNickname(type: string): string {
    return type
      .toLowerCase()
      .split('-')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
  }
}
