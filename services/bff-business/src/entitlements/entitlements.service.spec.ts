import { generateFixtures } from '@meridian/domain-fixtures';
import { EntitlementsService } from './entitlements.service';
import { EntitlementsClient } from '../clients/entitlements.client';
import { FixturesService } from '../clients/fixtures.service';
import { CacheService } from '../cache/cache.service';
import { Principal } from '../auth/principal';

describe('EntitlementsService', () => {
  const set = generateFixtures({ seed: 'meridian' });
  const fixtures = new FixturesService();
  const svc = new EntitlementsService(new EntitlementsClient(fixtures), new CacheService());
  const org = set.entitlements[0];
  const viewer = set.entitlements.find((e) => e.role === 'viewer');

  const principalFor = (userHandle: string, customerId = org.customerId): Principal => ({ subject: userHandle, customerId, segment: 'treasury', scopes: [] });

  it('merges the role baseline with explicit permissions', async () => {
    const ent = await svc.effective(principalFor(org.userHandle));
    expect(ent.role).toBe(org.role);
    expect(ent.effectivePermissions).toEqual(expect.arrayContaining(['accounts:view', ...org.permissions]));
  });

  it('denies a permission the role does not carry', async () => {
    if (!viewer) {
      return;
    }
    await expect(svc.require(principalFor(viewer.userHandle, viewer.customerId), 'payments:approve')).rejects.toMatchObject({ code: 'ENTITLEMENT_DENIED' });
  });

  it('refuses users with no entitlement in an organisation with none', async () => {
    const consumer = set.customers.find((c) => c.segment === 'consumer');
    await expect(svc.effective(principalFor('nobody', consumer?.customerId))).rejects.toMatchObject({ code: 'NO_ENTITLEMENT' });
  });

  it('decides on dual approval from the flag or the per transaction limit', () => {
    const base = { organisationId: 'o', userHandle: 'u', role: 'initiator' as const, permissions: [], dualApprovalRequired: false };
    expect(svc.needsSecondApprover({ ...base, dualApprovalRequired: true }, 1)).toBe(true);
    expect(svc.needsSecondApprover({ ...base, limitPerTransactionMinor: 100 }, 101)).toBe(true);
    expect(svc.needsSecondApprover({ ...base, limitPerTransactionMinor: 100 }, 100)).toBe(false);
    expect(svc.needsSecondApprover(base, 10_000_000)).toBe(false);
  });
});
