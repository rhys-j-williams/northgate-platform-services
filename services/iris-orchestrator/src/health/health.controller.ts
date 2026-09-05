import { Controller, Get } from '@nestjs/common';
import { Public } from '../auth/jwt-auth.guard';
import { CacheService } from '../cache/cache.service';
import { HandoffQueueService } from '../handoff/handoff-queue.service';
import { IntentLoader } from '../intents/intent-loader';
import { config } from '../config';

@Controller('health')
export class HealthController {
  constructor(private readonly cache: CacheService, private readonly queue: HandoffQueueService, private readonly loader: IntentLoader) {}

  @Public()
  @Get()
  live() {
    return { status: 'UP', service: config.serviceName, version: process.env.npm_package_version ?? 'dev' };
  }

  @Public()
  @Get('ready')
  ready() {
    return {
      status: 'UP',
      sessions: this.cache.mode,
      handoff: this.queue.mode,
      intents: this.loader.load().version,
      authMode: config.authMode,
    };
  }
}
