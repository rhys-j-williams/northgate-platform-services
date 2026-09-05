import { Global, Module } from '@nestjs/common';
import { FixturesService } from './fixtures.service';
import { BedrockClient } from './bedrock.client';
import { EntitlementsClient } from './entitlements.client';
import { ExposureClient } from './exposure.client';

const clients = [FixturesService, BedrockClient, EntitlementsClient, ExposureClient];

@Global()
@Module({ providers: clients, exports: clients })
export class ClientsModule {}
