import { Injectable, OnModuleDestroy } from '@nestjs/common';
import Redis from 'ioredis';
import { config } from '../config';
import { StructuredLogger } from '../common/logger';
import { Turn } from '../conversation/conversation.types';

export interface HandoffTicket {
  ticketId: string;
  sessionId: string;
  customerId: string;
  queue: string;
  reason: string;
  transcript: Turn[];
  correlationId: string;
  queuedAt: string;
}

/**
 * Pushes handoff tickets onto a Redis list that Semaphore (the agent desktop) BRPOPs. When Redis
 * is not there the tickets pile up in memory and are visible on /iris/v1/handoff/queue so the
 * demo can show them. In the bank a lost ticket is a complaint, so there the pod fails readiness
 * without Redis (helm/values-prod.yaml sets IRIS_HANDOFF_STRICT=true) - see runbook.
 */
@Injectable()
export class HandoffQueueService implements OnModuleDestroy {
  private readonly logger = new StructuredLogger('HandoffQueueService');
  private readonly local: HandoffTicket[] = [];
  private readonly redis: Redis | undefined;
  private redisHealthy = false;

  constructor() {
    this.redis = this.connect();
  }

  get mode(): 'redis' | 'memory' {
    return this.redisHealthy ? 'redis' : 'memory';
  }

  async enqueue(ticket: HandoffTicket): Promise<void> {
    const key = `${config.handoffQueueKey}:${ticket.queue}`;
    if (this.redisHealthy && this.redis) {
      try {
        await this.redis.lpush(key, JSON.stringify(ticket));
        this.logger.log(`handoff queued ticket=${ticket.ticketId} queue=${ticket.queue}`);
        return;
      } catch (err) {
        this.redisHealthy = false;
        this.logger.warn(`redis lpush failed, keeping ticket in memory: ${(err as Error).message}`);
      }
    }
    if (process.env.IRIS_HANDOFF_STRICT === 'true') {
      throw new Error('handoff queue unavailable');
    }
    this.local.push(ticket);
    this.logger.warn(`handoff ticket=${ticket.ticketId} held in memory (${this.local.length} waiting)`);
  }

  waiting(): HandoffTicket[] {
    return [...this.local];
  }

  depth(): number {
    return this.local.length;
  }

  async onModuleDestroy(): Promise<void> {
    this.redis?.disconnect();
  }

  private connect(): Redis | undefined {
    if (process.env.NODE_ENV === 'test' || config.redisUrl === 'memory') {
      return undefined;
    }
    const client = new Redis(config.redisUrl, { lazyConnect: true, maxRetriesPerRequest: 1, connectTimeout: 1500, retryStrategy: (t) => (t > 3 ? null : 500) });
    client.on('error', () => {
      this.redisHealthy = false;
    });
    client.on('ready', () => {
      this.redisHealthy = true;
    });
    client.connect().catch(() => {
      this.redisHealthy = false;
    });
    return client;
  }
}
