import { Injectable } from '@nestjs/common';
import { IntentDef, Match } from './intent.types';
import { IntentLoader } from './intent-loader';
import { config } from '../config';

/**
 * Keyword scoring. For each intent, score = sum of matched keyword weights / sqrt(number of
 * keywords), times the intent weight; the confidence reported is the top score normalised against
 * the runner up so that one clear hit reads as ~1.0 and a tie reads as ~0.5. It is crude and it is
 * what the transcripts were tuned against (PLAT-1433). Multi word keywords score double because
 * they are rarer. Regex keywords are written /like this/.
 */
@Injectable()
export class IntentMatcher {
  constructor(private readonly loader: IntentLoader) {}

  match(utterance: string): Match {
    const text = normalise(utterance);
    const scored = this.loader
      .all()
      .filter((i) => i.keywords.length > 0)
      .map((intent) => ({ intent, score: this.score(text, intent) }))
      .filter((s) => s.score > 0)
      .sort((a, b) => b.score - a.score);

    if (scored.length === 0) {
      return { intent: this.loader.fallback(), confidence: 0, entities: {} };
    }
    const top = scored[0];
    const next = scored[1]?.score ?? 0;
    const confidence = Math.min(1, top.score / (top.score + next));
    if (confidence < config.confidenceThreshold) {
      return { intent: this.loader.fallback(), confidence, entities: {} };
    }
    return { intent: top.intent, confidence, entities: this.entities(text, top.intent) };
  }

  private score(text: string, intent: IntentDef): number {
    let hits = 0;
    for (const kw of intent.keywords) {
      if (isRegex(kw)) {
        if (toRegex(kw).test(text)) {
          hits += 1;
        }
      } else if (kw.includes(' ')) {
        if (text.includes(kw)) {
          hits += 2;
        }
      } else if (hasToken(text, kw)) {
        hits += 1;
      }
    }
    if (hits === 0) {
      return 0;
    }
    return ((hits / Math.sqrt(intent.keywords.length)) * (intent.weight ?? 1));
  }

  private entities(text: string, intent: IntentDef): Record<string, string> {
    const out: Record<string, string> = {};
    for (const [name, pattern] of Object.entries(intent.entities ?? {})) {
      const m = toRegex(pattern).exec(text);
      if (m) {
        out[name] = m[1] ?? m[0];
      }
    }
    return out;
  }
}

export function normalise(s: string): string {
  return s
    .toLowerCase()
    .replace(/[^a-z0-9'\s$.]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function hasToken(text: string, token: string): boolean {
  return (' ' + text + ' ').includes(' ' + token + ' ');
}

function isRegex(kw: string): boolean {
  return kw.length > 2 && kw.startsWith('/') && kw.endsWith('/');
}

function toRegex(kw: string): RegExp {
  return isRegex(kw) ? new RegExp(kw.slice(1, -1)) : new RegExp(kw);
}
