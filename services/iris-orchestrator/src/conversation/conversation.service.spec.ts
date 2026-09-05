import { ConversationService } from './conversation.service';
import { CacheService } from '../cache/cache.service';
import { IntentLoader } from '../intents/intent-loader';
import { IntentMatcher } from '../intents/intent-matcher';
import { BffRetailClient } from '../clients/bff-retail.client';
import { HandoffQueueService } from '../handoff/handoff-queue.service';
import { FixturesService } from '../fixtures.service';
import { Principal } from '../auth/principal';

const principal: Principal = { subject: 'u1', customerId: 'CUS-100000', segment: 'consumer', scopes: [] };

describe('ConversationService', () => {
  let svc: ConversationService;
  let queue: HandoffQueueService;

  beforeEach(() => {
    const loader = new IntentLoader();
    queue = new HandoffQueueService();
    svc = new ConversationService(new CacheService(), new IntentMatcher(loader), loader, new BffRetailClient(new FixturesService()), queue);
  });

  it('starts with the greeting and quick replies', async () => {
    const r = await svc.start(principal);
    expect(r.intent).toBe('greeting');
    expect(r.quickReplies).toContain('Check my balance');
    expect(r.sessionId).toMatch(/[0-9a-f-]{36}/);
  });

  it('answers balance from fixtures when bff-retail is down', async () => {
    const s = await svc.start(principal);
    const r = await svc.message(principal, s.sessionId, 'what is my balance', 'x.y.z');
    expect(r.intent).toBe('balance');
    const data = r.data as Array<{ maskedNumber: string; available: string }>;
    expect(data.length).toBeGreaterThan(0);
    expect(data[0].maskedNumber).toMatch(/^\*{4}\d{4}$/);
  });

  it('hands off a dispute to the disputes queue', async () => {
    const s = await svc.start(principal);
    const r = await svc.message(principal, s.sessionId, 'this charge is not mine', 'x.y.z');
    expect(r.intent).toBe('dispute');
    expect(r.handoff?.queue).toBe('disputes');
    expect(r.disclosure).toBe('reg_e_dispute');
    expect(queue.depth()).toBe(1);
    expect(queue.waiting()[0].transcript.length).toBeGreaterThanOrEqual(3);
  });

  it('refuses another customer\'s session as not found', async () => {
    const s = await svc.start(principal);
    await expect(svc.message({ ...principal, customerId: 'CUS-999999' }, s.sessionId, 'hi', 't')).rejects.toMatchObject({ code: 'SESSION_NOT_FOUND' });
  });
});
