import { Injectable } from '@nestjs/common';
import { Entitlement } from '@meridian/domain-fixtures';
import { UpstreamClient } from './upstream-client';
import { FixturesService } from './fixtures.service';
import { config } from '../config';

export interface EntitlementView {
  organisationId: string;
  userHandle: string;
  role: Entitlement['role'];
  permissions: string[];
  dualApprovalRequired: boolean;
  limitPerTransactionMinor?: number;
  limitPerDayMinor?: number;
}

/**
 * entitlements-service is the only backend already on Boot 3 / Java 17; its API is versioned
 * under /entitlements/v1 and returns the same shape as the fixture Entitlement, so the fallback
 * is a straight filter.
 */
@Injectable()
export class EntitlementsClient extends UpstreamClient {
  constructor(private readonly fixtures: FixturesService) {
    super('entitlements', config.entitlementsUrl);
  }

  forUser(customerId: string, userHandle: string): Promise<EntitlementView[]> {
    return this.callOrFallback(
      async () => (await this.http.get<EntitlementView[]>(`/users/${encodeURIComponent(userHandle)}/entitlements`, { params: { customerId } })).data,
      () => {
        const all = this.fixtures.get().entitlements.filter((e) => e.customerId === customerId);
        const mine = all.filter((e) => e.userHandle === userHandle);
        // fixture users are not the Keystone subject; fall back to the org's first administrator so the demo user can see something
        return (mine.length > 0 ? mine : all.filter((e) => e.role === 'administrator').slice(0, 1)).map((e) => ({
          organisationId: e.organisationId,
          userHandle: e.userHandle,
          role: e.role,
          permissions: e.permissions,
          dualApprovalRequired: e.dualApprovalRequired,
          limitPerTransactionMinor: e.limitPerTransactionMinor,
          limitPerDayMinor: e.limitPerDayMinor,
        }));
      },
    );
  }

  forOrganisation(customerId: string): Promise<EntitlementView[]> {
    return this.callOrFallback(
      async () => (await this.http.get<EntitlementView[]>(`/organisations/${customerId}/entitlements`)).data,
      () => this.fixtures.get().entitlements.filter((e) => e.customerId === customerId),
    );
  }
}
