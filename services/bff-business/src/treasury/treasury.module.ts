import { Module } from '@nestjs/common';
import { EntitlementsModule } from '../entitlements/entitlements.module';
import { TreasuryController } from './treasury.controller';

@Module({ imports: [EntitlementsModule], controllers: [TreasuryController] })
export class TreasuryModule {}
