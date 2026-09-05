import { generateFixtures } from '@meridian/domain-fixtures';
import { AccountsService } from './accounts.service';
import { BedrockClient } from '../clients/bedrock.client';
import { CacheService } from '../cache/cache.service';
import { FixturesService } from '../clients/fixtures.service';
import { Principal } from '../auth/principal';
import { categoryForMcc, money } from './money';

describe('AccountsService', () => {
  const fixtures = new FixturesService();
  const set = generateFixtures({ seed: 'meridian' });
  const customer = set.customers[0];
  const principal: Principal = { subject: 'u', customerId: customer.customerId, segment: 'consumer', scopes: [] };
  // upstream is unreachable in tests (see test/setup-env.ts) so BedrockClient serves fixtures
  const svc = new AccountsService(new BedrockClient(fixtures), new CacheService(), fixtures);

  it('lists accounts with masked numbers and formatted money', async () => {
    const accounts = await svc.list(principal);
    expect(accounts.length).toBeGreaterThan(0);
    for (const a of accounts) {
      expect(a.maskedNumber).toMatch(/^\D+\d{4}$/);
      expect(a.currentBalance.amount).toMatch(/^-?\d+\.\d{2}$/);
    }
  });

  it('dashboard splits deposits and borrowing', async () => {
    const d = await svc.dashboard(principal);
    expect(d.displayName).toBe(customer.displayName);
    expect(d.totals.deposits.minor + d.totals.borrowing.minor).toBe(d.accounts.reduce((s, a) => s + a.currentBalance.minor, 0));
  });

  it('hides accounts owned by another customer as 404', async () => {
    const other = set.accounts.filter((a) => a.customerId !== customer.customerId)[0];
    await expect(svc.get(principal, other.accountId)).rejects.toMatchObject({ code: 'ACCOUNT_NOT_FOUND' });
  });

  it('returns transactions newest first with categories', async () => {
    const [first] = await svc.list(principal);
    const txns = await svc.transactions(principal, first.accountId, 10);
    expect(txns.length).toBeLessThanOrEqual(10);
    for (let i = 1; i < txns.length; i++) {
      expect(txns[i - 1].postedOn >= txns[i].postedOn).toBe(true);
    }
    expect(txns.every((t) => typeof t.category === 'string')).toBe(true);
  });

  it('formats minor units', () => {
    expect(money(-275969).amount).toBe('-2759.69');
    expect(money(5).amount).toBe('0.05');
    expect(categoryForMcc('5411')).toBe('groceries');
    expect(categoryForMcc('9999')).toBe('other');
  });
});
