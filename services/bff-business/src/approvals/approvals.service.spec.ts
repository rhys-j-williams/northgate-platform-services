import { ApprovalsService } from './approvals.service';
import { EntitlementsService } from '../entitlements/entitlements.service';
import { CacheService } from '../cache/cache.service';
import { Principal } from '../auth/principal';
import { EntitlementView } from '../clients/entitlements.client';

/*
 * Only the four eyes rule is covered here. Expiry, rejection, the daily limit check and the
 * (missing) release to txn-posting are not. PLAT-2019 / PLAT-1988.
 */
describe('ApprovalsService four eyes', () => {
  const initiator: Principal = { subject: 'init', customerId: 'CUS-1', segment: 'treasury', scopes: [] };
  const approver: Principal = { subject: 'appr', customerId: 'CUS-1', segment: 'treasury', scopes: [] };
  const ent: EntitlementView = { organisationId: 'ORG-1', userHandle: 'x', role: 'administrator', permissions: [], dualApprovalRequired: true };

  const entitlements = {
    require: jest.fn().mockResolvedValue(ent),
    needsSecondApprover: jest.fn().mockReturnValue(true),
  } as unknown as EntitlementsService;

  it('does not let the initiator approve their own payment and needs two distinct approvers', async () => {
    const svc = new ApprovalsService(new CacheService(), entitlements);
    const item = await svc.submitPayment(initiator, { fromAccountId: 'ACC-000000001', amountMinor: 500_000, beneficiary: 'Hollow Creek Cabinetry' });
    expect(item.requiredApprovals).toBe(2);

    await expect(svc.approve(initiator, item.approvalId)).rejects.toMatchObject({ code: 'FOUR_EYES' });

    const once = await svc.approve(approver, item.approvalId);
    expect(once.status).toBe('PENDING');
    await expect(svc.approve(approver, item.approvalId)).rejects.toMatchObject({ code: 'ALREADY_APPROVED' });

    const done = await svc.approve({ ...approver, subject: 'appr2' }, item.approvalId);
    expect(done.status).toBe('APPROVED');
    expect((await svc.list(approver, 'APPROVED')).map((a) => a.approvalId)).toEqual([item.approvalId]);
  });
});
