import { Injectable } from '@nestjs/common';
import { randomUUID } from 'crypto';
import { CacheService } from '../cache/cache.service';
import { IntentMatcher } from '../intents/intent-matcher';
import { IntentLoader } from '../intents/intent-loader';
import { BffRetailClient } from '../clients/bff-retail.client';
import { HandoffQueueService } from '../handoff/handoff-queue.service';
import { Principal } from '../auth/principal';
import { ApiException } from '../common/api-error';
import { correlation } from '../common/correlation';
import { Reply, Session, Turn } from './conversation.types';
import { IntentDef, Match } from '../intents/intent.types';
import { config } from '../config';

const MAX_MISSES = 3;

@Injectable()
export class ConversationService {
  constructor(
    private readonly cache: CacheService,
    private readonly matcher: IntentMatcher,
    private readonly loader: IntentLoader,
    private readonly bff: BffRetailClient,
    private readonly handoff: HandoffQueueService,
  ) {}

  async start(principal: Principal): Promise<Reply> {
    const session: Session = {
      sessionId: randomUUID(),
      customerId: principal.customerId,
      subject: principal.subject,
      startedAt: new Date().toISOString(),
      turns: [],
      handedOff: false,
      ended: false,
      misses: 0,
    };
    const greeting = this.loader.byId('greeting') ?? this.loader.fallback();
    const reply = this.speak(session, greeting, 1, undefined);
    await this.save(session);
    return reply;
  }

  async message(principal: Principal, sessionId: string, text: string, bearer: string): Promise<Reply> {
    const session = await this.load(principal, sessionId);
    if (session.ended) {
      throw ApiException.conflict('SESSION_ENDED', 'this conversation has ended, start a new one');
    }
    session.turns.push({ at: now(), from: 'customer', text });

    let match: Match = this.matcher.match(text);
    if (match.intent.id === this.loader.fallback().id) {
      session.misses += 1;
      if (session.misses >= MAX_MISSES) {
        match = { intent: this.loader.byId('human') ?? match.intent, confidence: match.confidence, entities: {} };
      }
    } else {
      session.misses = 0;
    }

    let data: unknown;
    if (match.intent.action) {
      data = await this.act(match, principal, bearer);
    }
    const reply = this.speak(session, match.intent, match.confidence, data);

    if (match.intent.handoff) {
      const ticketId = `IRS-${Date.now().toString(36).toUpperCase()}`;
      await this.handoff.enqueue({
        ticketId,
        sessionId: session.sessionId,
        customerId: session.customerId,
        queue: match.intent.handoff_queue ?? 'general',
        reason: match.intent.id,
        transcript: session.turns,
        correlationId: correlation.current(),
        queuedAt: now(),
      });
      session.handedOff = true;
      reply.handoff = { queue: match.intent.handoff_queue ?? 'general', ticketId };
      session.turns.push({ at: now(), from: 'system', text: `handoff ${ticketId}` });
    }
    if (match.intent.end) {
      session.ended = true;
      reply.ended = true;
    }
    await this.save(session);
    return reply;
  }

  async transcript(principal: Principal, sessionId: string): Promise<Turn[]> {
    return (await this.load(principal, sessionId)).turns;
  }

  private async act(match: Match, principal: Principal, bearer: string): Promise<unknown> {
    switch (match.intent.action) {
      case 'balances':
        return this.bff.balances(bearer, principal.customerId);
      case 'recent_transactions':
        return this.bff.recentTransactions(bearer, principal.customerId, match.entities['last4']);
      default:
        return undefined;
    }
  }

  private speak(session: Session, intent: IntentDef, confidence: number, data: unknown): Reply {
    // Fallback rotates its lines so the customer does not get the same apology three times.
    const idx = intent.responses.length === 1 ? 0 : session.misses % intent.responses.length;
    const text = intent.responses[idx];
    session.turns.push({ at: now(), from: 'iris', text, intent: intent.id, confidence });
    return {
      sessionId: session.sessionId,
      intent: intent.id,
      confidence: Number(confidence.toFixed(2)),
      messages: [text],
      quickReplies: intent.quick_replies ?? [],
      disclosure: intent.disclosure,
      data,
      ended: false,
    };
  }

  private async load(principal: Principal, sessionId: string): Promise<Session> {
    const s = await this.cache.get<Session>(key(sessionId));
    if (!s) {
      throw ApiException.notFound('SESSION_NOT_FOUND', `no session ${sessionId} (expired after ${config.cacheTtlSeconds}s?)`);
    }
    if (s.customerId !== principal.customerId) {
      // Same as a miss. Do not tell them it exists (GIS-1204).
      throw ApiException.notFound('SESSION_NOT_FOUND', `no session ${sessionId}`);
    }
    return s;
  }

  private async save(s: Session): Promise<void> {
    await this.cache.set(key(s.sessionId), s, config.cacheTtlSeconds);
  }
}

function key(sessionId: string): string {
  return `iris:session:${sessionId}`;
}

function now(): string {
  return new Date().toISOString();
}
