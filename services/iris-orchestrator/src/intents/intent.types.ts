export interface IntentDef {
  id: string;
  keywords: string[];
  weight?: number;
  requires_auth?: boolean;
  action?: 'balances' | 'recent_transactions';
  entities?: Record<string, string>;
  handoff?: boolean;
  handoff_queue?: string;
  disclosure?: string;
  responses: string[];
  quick_replies?: string[];
  end?: boolean;
}

export interface IntentFile {
  version: number;
  fallback: string;
  intents: IntentDef[];
}

export interface Match {
  intent: IntentDef;
  confidence: number;
  entities: Record<string, string>;
}
