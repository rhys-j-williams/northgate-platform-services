import { Injectable } from '@nestjs/common';
import { randomUUID } from 'crypto';
import { Approval } from './approval';
import { CacheService } from '../cache/cache.service';
import { EntitlementsService } from '../entitlements/entitlements.service';
import { ApiException } from '../common/api-error';
import { Principal } from '../auth/principal';
import { StructuredLogger } from '../common/logger';

const QUEUE_TTL_SECONDS = 7 * 86_400;

/**
 * Approvals queue. Items sit in Redis keyed by organisation; ledgerline-web polls the list. The
 * queue was supposed to move to a table in entitlements-service when that went to Boot 3
 * (PLAT-1988) so that it survives a Redis flush; it did not, and a flush during the 2024.10
 * train silently dropped four pending wires (INC0050311). Keep the TTL long and do not "tidy".
 *
 * Four eyes: an initiator can never approve their own item, and when requiredApprovals is 2 the
 * two approvers must be distinct.
 */
@Injectable()
export class ApprovalsService {
  private readonly logger = new StructuredLogger('ApprovalsService');

  constructor(private readonly cache: CacheService, private readonly entitlements: EntitlementsService) {}

  async list(principal: Principal, status?: Approval['status']): Promise<Approval[]> {
    await this.entitlements.require(principal, 'accounts:view');
    const all = await this.load(principal.customerId);
    const now = Date.now();
    for (const a of all) {
      if (a.status === 'PENDING' && new Date(a.expiresAt).getTime() < now) {
        a.status = 'EXPIRED';
      }
    }
    await this.save(principal.customerId, all);
    return status ? all.filter((a) => a.status === status) : all;
  }

  async submitPayment(principal: Principal, input: { fromAccountId: string; amountMinor: number; beneficiary: string; reference?: string; valueDate?: string }): Promise<Approval> {
    const ent = await this.entitlements.require(principal, 'payments:initiate');
    if (ent.limitPerDayMinor !== undefined && input.amountMinor > ent.limitPerDayMinor) {
      throw ApiException.conflict('LIMIT_EXCEEDED', 'amount exceeds the initiator daily limit');
    }
    const all = await this.load(principal.customerId);
    const approval: Approval = {
      approvalId: `APR-${randomUUID().slice(0, 8).toUpperCase()}`,
      customerId: principal.customerId,
      organisationId: ent.organisationId,
      kind: 'payment',
      summary: `Wire ${(input.amountMinor / 100).toFixed(2)} USD to ${input.beneficiary}`,
      amountMinor: input.amountMinor,
      fromAccountId: input.fromAccountId,
      payload: { ...input },
      initiatedBy: principal.subject,
      initiatedAt: new Date().toISOString(),
      requiredApprovals: this.entitlements.needsSecondApprover(ent, input.amountMinor) ? 2 : 1,
      approvals: [],
      status: 'PENDING',
      expiresAt: new Date(Date.now() + 2 * 86_400_000).toISOString(),
    };
    all.unshift(approval);
    await this.save(principal.customerId, all);
    this.logger.log(`approval ${approval.approvalId} submitted, ${approval.requiredApprovals} required`);
    return approval;
  }

  async approve(principal: Principal, approvalId: string): Promise<Approval> {
    await this.entitlements.require(principal, 'payments:approve');
    const all = await this.load(principal.customerId);
    const item = this.find(all, approvalId);
    if (item.status !== 'PENDING') {
      throw ApiException.conflict('APPROVAL_NOT_PENDING', `approval is ${item.status}`);
    }
    if (item.initiatedBy === principal.subject) {
      throw ApiException.forbidden('FOUR_EYES', 'initiator cannot approve their own item');
    }
    if (item.approvals.some((a) => a.by === principal.subject)) {
      throw ApiException.conflict('ALREADY_APPROVED', 'you have already approved this item');
    }
    item.approvals.push({ by: principal.subject, at: new Date().toISOString() });
    if (item.approvals.length >= item.requiredApprovals) {
      item.status = 'APPROVED';
      // release to txn-posting-service happens in ReleaseWorker (not in this repo slice, PLAT-2019)
    }
    await this.save(principal.customerId, all);
    return item;
  }

  async reject(principal: Principal, approvalId: string, reason: string): Promise<Approval> {
    await this.entitlements.require(principal, 'payments:approve');
    const all = await this.load(principal.customerId);
    const item = this.find(all, approvalId);
    if (item.status !== 'PENDING') {
      throw ApiException.conflict('APPROVAL_NOT_PENDING', `approval is ${item.status}`);
    }
    item.status = 'REJECTED';
    item.rejection = { by: principal.subject, at: new Date().toISOString(), reason };
    await this.save(principal.customerId, all);
    return item;
  }

  private find(all: Approval[], approvalId: string): Approval {
    const item = all.find((a) => a.approvalId === approvalId);
    if (!item) {
      throw ApiException.notFound('APPROVAL_NOT_FOUND', `approval ${approvalId} not found`);
    }
    return item;
  }

  private async load(customerId: string): Promise<Approval[]> {
    return (await this.cache.get<Approval[]>(`approvals:${customerId}`)) ?? [];
  }

  private save(customerId: string, all: Approval[]): Promise<void> {
    return this.cache.set(`approvals:${customerId}`, all, QUEUE_TTL_SECONDS);
  }
}
