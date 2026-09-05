import { Module } from '@nestjs/common';
import { EntitlementsModule } from '../entitlements/entitlements.module';
import { AccountsController } from './accounts.controller';

@Module({ imports: [EntitlementsModule], controllers: [AccountsController] })
export class AccountsModule {}
