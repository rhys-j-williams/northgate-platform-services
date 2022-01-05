import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { AuthModule } from './auth/auth.module';
import { JwtAuthGuard } from './auth/jwt-auth.guard';
import { CacheModule } from './cache/cache.module';
import { ClientsModule } from './clients/clients.module';
import { EntitlementsModule } from './entitlements/entitlements.module';
import { ApprovalsModule } from './approvals/approvals.module';
import { AccountsModule } from './accounts/accounts.module';
import { TreasuryModule } from './treasury/treasury.module';
import { HealthModule } from './health/health.module';

@Module({
  imports: [AuthModule, CacheModule, ClientsModule, EntitlementsModule, ApprovalsModule, AccountsModule, TreasuryModule, HealthModule],
  providers: [{ provide: APP_GUARD, useClass: JwtAuthGuard }],
})
export class AppModule {}
