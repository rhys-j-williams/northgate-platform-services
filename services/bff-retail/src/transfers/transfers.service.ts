import { Injectable } from '@nestjs/common';
import { ApiException } from '../common/api-error';
import { CacheService } from '../cache/cache.service';
import { PostingReceipt, TxnPostingClient } from '../clients/txn-posting.client';
import { AccountsService } from '../accounts/accounts.service';
import { Principal } from '../auth/principal';
import { CreateTransferDto } from './transfer.dto';

/** Transfers above this need an MFA claim younger than ten minutes. Mirrors retail-web MfaStepUpGuard; keep in step (MOL-3310). */
export const MFA_STEP_UP_THRESHOLD_MINOR = 250_000;
const MFA_MAX_AGE_SECONDS = 600;

@Injectable()
export class TransfersService {
  constructor(private readonly posting: TxnPostingClient, private readonly accounts: AccountsService, private readonly cache: CacheService) {}

  async create(principal: Principal, dto: CreateTransferDto): Promise<PostingReceipt> {
    if (!dto.toAccountId && !dto.toPayeeId) {
      throw ApiException.badRequest('TRANSFER_NO_DESTINATION', 'toAccountId or toPayeeId required');
    }
    if (dto.toAccountId === dto.fromAccountId) {
      throw ApiException.badRequest('TRANSFER_SAME_ACCOUNT', 'from and to accounts are the same');
    }
    if (dto.amountMinor >= MFA_STEP_UP_THRESHOLD_MINOR) {
      const age = principal.mfaAt === undefined ? Number.POSITIVE_INFINITY : Date.now() / 1000 - principal.mfaAt;
      if (age > MFA_MAX_AGE_SECONDS) {
        throw ApiException.forbidden('MFA_STEP_UP_REQUIRED', 'transfer above threshold requires recent MFA');
      }
    }
    const from = await this.accounts.get(principal, dto.fromAccountId);
    if (from.status !== 'OPEN') {
      throw ApiException.conflict('ACCOUNT_NOT_OPEN', `account ${from.accountId} is ${from.status}`);
    }
    if (!dto.scheduledFor && from.availableBalance.minor < dto.amountMinor) {
      throw ApiException.conflict('INSUFFICIENT_FUNDS', 'available balance is below the transfer amount');
    }

    // BFF side replay guard. Not authoritative (see CacheService note on per pod memory); the
    // real idempotency ledger is in txn-posting-service. PLAT-2088.
    const replayKey = `transfer:${principal.customerId}:${dto.idempotencyKey}`;
    const previous = await this.cache.get<PostingReceipt>(replayKey);
    if (previous) {
      return previous;
    }

    const receipt = await this.posting.post({
      idempotencyKey: dto.idempotencyKey,
      customerId: principal.customerId,
      fromAccountId: dto.fromAccountId,
      toAccountId: dto.toAccountId,
      toPayeeId: dto.toPayeeId,
      amountMinor: dto.amountMinor,
      memo: dto.memo,
      scheduledFor: dto.scheduledFor,
    });
    await this.cache.set(replayKey, receipt, 86_400);
    await this.cache.del(`accounts:${principal.customerId}`);
    await this.cache.del(`account:${dto.fromAccountId}`);
    if (dto.toAccountId) {
      await this.cache.del(`account:${dto.toAccountId}`);
    }
    return receipt;
  }

  limits(principal: Principal) {
    // Static for consumer; small-business limits come from entitlements-service in bff-business.
    return {
      customerId: principal.customerId,
      perTransactionMinor: 1_000_000,
      perDayMinor: 2_500_000,
      mfaStepUpAboveMinor: MFA_STEP_UP_THRESHOLD_MINOR,
    };
  }
}
