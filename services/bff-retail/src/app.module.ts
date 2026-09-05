import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { AuthModule } from './auth/auth.module';
import { JwtAuthGuard } from './auth/jwt-auth.guard';
import { CacheModule } from './cache/cache.module';
import { ClientsModule } from './clients/clients.module';
import { AccountsModule } from './accounts/accounts.module';
import { TransfersModule } from './transfers/transfers.module';
import { CardsModule } from './cards/cards.module';
import { PayeesModule } from './payees/payees.module';
import { MarketsModule } from './markets/markets.module';
import { PayLinkModule } from './paylink/paylink.module';
import { HealthModule } from './health/health.module';

@Module({
  imports: [
    AuthModule,
    CacheModule,
    ClientsModule,
    AccountsModule,
    TransfersModule,
    CardsModule,
    PayeesModule,
    MarketsModule,
    PayLinkModule,
    HealthModule,
  ],
  providers: [{ provide: APP_GUARD, useClass: JwtAuthGuard }],
})
export class AppModule {}
