import { Injectable } from '@nestjs/common';
import { EntitlementsClient, EntitlementView } from '../clients/entitlements.client';
import { CacheService } from '../cache/cache.service';
import { ApiException } from '../common/api-error';
import { Principal } from '../auth/principal';

export type Permission = 'payments:initiate' | 'payments:approve' | 'accounts:view' | 'users:manage' | 'reports:view' | 'positions:view';

/**
 * Effective entitlement for the caller. Permission strings come from entitlements-service; the
 * role implies a baseline set so that a user with an empty permission list is not locked out
 * (this happened for every treasury user after the 2023.11 migration, INC0047102).
 */
@Injectable()
export class EntitlementsService {
  private static readonly ROLE_BASELINE: Record<EntitlementView['role'], Permission[]> = {
    administrator: ['payments:initiate', 'payments:approve', 'accounts:view', 'users:manage', 'reports:view', 'positions:view'],
    approver: ['payments:approve', 'accounts:view', 'reports:view', 'positions:view'],
    initiator: ['payments:initiate', 'accounts:view'],
    viewer: ['accounts:view'],
    auditor: ['accounts:view', 'reports:view'],
  };

  constructor(private readonly client: EntitlementsClient, private readonly cache: CacheService) {}

  async effective(principal: Principal): Promise<EntitlementView & { effectivePermissions: string[] }> {
    const list = await this.cache.getOrLoad(`ent:${principal.customerId}:${principal.subject}`, () => this.client.forUser(principal.customerId, principal.subject), 120);
    if (list.length === 0) {
      throw ApiException.forbidden('NO_ENTITLEMENT', 'user has no entitlement for this organisation');
    }
    const primary = list[0];
    const effectivePermissions = Array.from(new Set([...EntitlementsService.ROLE_BASELINE[primary.role], ...primary.permissions]));
    return { ...primary, effectivePermissions };
  }

  async require(principal: Principal, permission: Permission): Promise<EntitlementView> {
    const ent = await this.effective(principal);
    if (!ent.effectivePermissions.includes(permission)) {
      throw ApiException.forbidden('ENTITLEMENT_DENIED', `${permission} not granted to ${ent.role}`);
    }
    return ent;
  }

  /** Dual approval applies when the entitlement says so or the amount is over the user's own limit. */
  needsSecondApprover(ent: EntitlementView, amountMinor: number): boolean {
    if (ent.dualApprovalRequired) {
      return true;
    }
    return ent.limitPerTransactionMinor !== undefined && amountMinor > ent.limitPerTransactionMinor;
  }
}
