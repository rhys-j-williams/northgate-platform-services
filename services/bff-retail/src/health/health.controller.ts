import { Controller, Get } from '@nestjs/common';
import { Public } from '../auth/jwt-auth.guard';
import { CacheService } from '../cache/cache.service';
import { config } from '../config';

@Controller('health')
export class HealthController {
  constructor(private readonly cache: CacheService) {}

  @Public()
  @Get()
  live() {
    return { status: 'UP', service: config.serviceName, version: process.env.npm_package_version ?? 'dev' };
  }

  @Public()
  @Get('ready')
  ready() {
    // Redis down is degraded, not unready: we serve from memory. The OpenShift readiness probe
    // therefore never flaps on Redis, which was the point of INC0049930.
    return { status: 'UP', cache: this.cache.mode, authMode: config.authMode, fixtureFallback: config.fixtureFallback };
  }
}
