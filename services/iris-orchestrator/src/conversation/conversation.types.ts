export interface Turn {
  at: string;
  from: 'customer' | 'iris' | 'system';
  text: string;
  intent?: string;
  confidence?: number;
}

export interface Session {
  sessionId: string;
  customerId: string;
  subject: string;
  startedAt: string;
  turns: Turn[];
  handedOff: boolean;
  ended: boolean;
  /** consecutive fallbacks; three in a row forces a handoff (PLAT-1433) */
  misses: number;
}

export interface Reply {
  sessionId: string;
  intent: string;
  confidence: number;
  messages: string[];
  quickReplies: string[];
  handoff?: { queue: string; ticketId: string };
  disclosure?: string;
  data?: unknown;
  ended: boolean;
}
