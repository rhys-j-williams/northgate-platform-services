import { MiddlewareConsumer, Module, NestModule } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { AuthModule } from './auth/auth.module';
import { JwtAuthGuard } from './auth/jwt-auth.guard';
import { CacheModule } from './cache/cache.module';
import { CorrelationMiddleware } from './common/correlation.middleware';
import { FixturesService } from './fixtures.service';
import { IntentLoader } from './intents/intent-loader';
import { IntentMatcher } from './intents/intent-matcher';
import { IntentsController } from './intents/intents.controller';
import { BffRetailClient } from './clients/bff-retail.client';
import { HandoffQueueService } from './handoff/handoff-queue.service';
import { HandoffController } from './handoff/handoff.controller';
import { ConversationService } from './conversation/conversation.service';
import { ConversationController } from './conversation/conversation.controller';
import { HealthController } from './health/health.controller';

// One module. It was four; PLAT-1590 flattened it because nobody could find anything.
@Module({
  imports: [AuthModule, CacheModule],
  controllers: [ConversationController, IntentsController, HandoffController, HealthController],
  providers: [
    FixturesService,
    IntentLoader,
    IntentMatcher,
    BffRetailClient,
    HandoffQueueService,
    ConversationService,
    { provide: APP_GUARD, useClass: JwtAuthGuard },
  ],
})
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer): void {
    consumer.apply(CorrelationMiddleware).forRoutes('*');
  }
}
