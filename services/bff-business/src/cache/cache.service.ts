import { Injectable, OnModuleDestroy, Optional } from '@nestjs/common';
import Redis from 'ioredis';
import { config } from '../config';
import { StructuredLogger } from '../common/logger';

interface Entry {
  value: string;
  expiresAt: number;
}

/**
 * Read through cache in front of the aggregated upstreams. Redis in every environment above local;
 * if Redis is not reachable we degrade to an in process map rather than failing requests, because
 * a Redis restart during the 2024.06 train took retail login down for eleven minutes (INC0049930).
 * The in process map is per pod and is NOT shared, which matters for the transfer idempotency
 * keys (see TransfersService) - PLAT-2088 tracks moving those to txn-posting-service.
 */
@Injectable()
export class CacheService implements OnModuleDestroy {
  private readonly logger = new StructuredLogger('CacheService');
  private readonly local = new Map<string, Entry>();
  private redis: Redis | undefined;
  private redisHealthy = false;

  constructor(@Optional() client?: Redis) {
    this.redis = client ?? this.connect();
  }

  get mode(): 'redis' | 'memory' {
    return this.redisHealthy ? 'redis' : 'memory';
  }

  async get<T>(key: string): Promise<T | undefined> {
    const raw = await this.getRaw(key);
    return raw === undefined ? undefined : (JSON.parse(raw) as T);
  }

  async set(key: string, value: unknown, ttlSeconds = config.cacheTtlSeconds): Promise<void> {
    const raw = JSON.stringify(value);
    if (this.redisHealthy && this.redis) {
      try {
        await this.redis.set(key, raw, 'EX', ttlSeconds);
        return;
      } catch (err) {
        this.markUnhealthy(err);
      }
    }
    this.local.set(key, { value: raw, expiresAt: Date.now() + ttlSeconds * 1000 });
  }

  async del(key: string): Promise<void> {
    this.local.delete(key);
    if (this.redisHealthy && this.redis) {
      try {
        await this.redis.del(key);
      } catch (err) {
        this.markUnhealthy(err);
      }
    }
  }

  async getOrLoad<T>(key: string, loader: () => Promise<T>, ttlSeconds?: number): Promise<T> {
    const hit = await this.get<T>(key);
    if (hit !== undefined) {
      return hit;
    }
    const value = await loader();
    await this.set(key, value, ttlSeconds);
    return value;
  }

  async onModuleDestroy(): Promise<void> {
    if (this.redis) {
      this.redis.disconnect();
    }
  }

  private async getRaw(key: string): Promise<string | undefined> {
    if (this.redisHealthy && this.redis) {
      try {
        const v = await this.redis.get(key);
        return v === null ? undefined : v;
      } catch (err) {
        this.markUnhealthy(err);
      }
    }
    const entry = this.local.get(key);
    if (!entry) {
      return undefined;
    }
    if (entry.expiresAt < Date.now()) {
      this.local.delete(key);
      return undefined;
    }
    return entry.value;
  }

  private connect(): Redis | undefined {
    if (process.env.NODE_ENV === 'test' || config.redisUrl === 'memory') {
      return undefined;
    }
    const client = new Redis(config.redisUrl, {
      lazyConnect: true,
      maxRetriesPerRequest: 1,
      connectTimeout: 1500,
      retryStrategy: (times) => (times > 3 ? null : 500),
    });
    client.on('error', (err: Error) => this.markUnhealthy(err));
    client.on('ready', () => {
      this.redisHealthy = true;
      this.logger.log('redis connected');
    });
    client.connect().catch((err: Error) => this.markUnhealthy(err));
    return client;
  }

  private markUnhealthy(err: unknown): void {
    if (this.redisHealthy || this.local.size === 0) {
      this.logger.warn(`redis unavailable, using in process cache: ${(err as Error).message}`);
    }
    this.redisHealthy = false;
  }
}
