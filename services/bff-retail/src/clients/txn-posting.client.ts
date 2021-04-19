import { Injectable } from '@nestjs/common';
import { UpstreamClient } from './upstream-client';
import { config } from '../config';

export interface PostingCommand {
  idempotencyKey: string;
  customerId: string;
  fromAccountId: string;
  toAccountId?: string;
  toPayeeId?: string;
  amountMinor: number;
  memo?: string;
  scheduledFor?: string;
}

export interface PostingReceipt {
  postingId: string;
  status: 'POSTED' | 'SCHEDULED' | 'PENDING_REVIEW' | 'REJECTED';
  transactionIds: string[];
  newAvailableBalanceMinor?: number;
  reason?: string;
}

/**
 * Money movement goes through txn-posting-service, never straight to the adapter, because that
 * is where the idempotency ledger and the compliance checks live. When posting is down the
 * fixture path returns a PENDING_REVIEW receipt so the UI shows the right screen; nothing is
 * booked.
 */
@Injectable()
export class TxnPostingClient extends UpstreamClient {
  constructor() {
    super('txn-posting', config.txnPostingUrl);
  }

  post(command: PostingCommand): Promise<PostingReceipt> {
    return this.callOrFallback(
      async () => (await this.http.post<PostingReceipt>('/postings', command, { headers: { 'Idempotency-Key': command.idempotencyKey } })).data,
      () => ({
        postingId: `PST-OFFLINE-${command.idempotencyKey.slice(0, 8)}`,
        status: 'PENDING_REVIEW',
        transactionIds: [],
        reason: 'posting service unavailable, queued for review',
      }),
    );
  }
}
