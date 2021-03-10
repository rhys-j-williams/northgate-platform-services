import { Global, Module } from '@nestjs/common';
import { FixturesService } from './fixtures.service';
import { BedrockClient } from './bedrock.client';
import { AggregioClient, PayLinkClient, TickerHausClient, TriScoreClient } from './partner.clients';
import { TxnPostingClient } from './txn-posting.client';

const clients = [FixturesService, BedrockClient, AggregioClient, TickerHausClient, TriScoreClient, PayLinkClient, TxnPostingClient];

@Global()
@Module({ providers: clients, exports: clients })
export class ClientsModule {}
