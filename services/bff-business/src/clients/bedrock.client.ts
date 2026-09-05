import { Injectable } from '@nestjs/common';
import { UpstreamClient } from './upstream-client';
import { FixturesService } from './fixtures.service';
import { config } from '../config';

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

@Injectable()
export class BedrockClient extends UpstreamClient {
  constructor(private readonly fixtures: FixturesService) {
    super('bedrock-adapter', config.bedrockAdapterUrl);
  }

  accountsForCustomer(customerId: string): Promise<BedrockAccount[]> {
    return this.callOrFallback(
      async () => (await this.http.get<BedrockAccount[]>(`/customers/${customerId}/accounts`)).data,
      () => {
        const set = this.fixtures.get();
        const owner = set.customers.find((c) => c.customerId === customerId);
        return set.accounts
          .filter((a) => a.customerId === customerId)
          .map((a) => ({
            accountId: a.accountId,
            customerId: a.customerId,
            type: a.type.toUpperCase(),
            accountNumber: a.accountNumber,
            routingNumber: a.routingNumber,
            currentBalanceMinor: a.currentBalanceMinor,
            availableBalanceMinor: a.availableBalanceMinor,
            openedDate: a.openedAt.slice(0, 10),
            status: a.status.toUpperCase(),
            ownerName: owner?.organisationName ?? owner?.displayName ?? '',
          }));
      },
    );
  }
}
