import { Injectable } from '@nestjs/common';
import { readFileSync } from 'fs';
import { join } from 'path';
import * as yaml from 'js-yaml';
import { IntentDef, IntentFile } from './intent.types';
import { config } from '../config';
import { StructuredLogger } from '../common/logger';

/**
 * Loads intents.yaml once at startup. Hot reload was removed in PLAT-1210 after a half written
 * file on the shared volume took the widget down; deploy a new pod instead.
 */
@Injectable()
export class IntentLoader {
  private readonly logger = new StructuredLogger('IntentLoader');
  private file: IntentFile | undefined;

  load(): IntentFile {
    if (this.file) {
      return this.file;
    }
    const path = config.intentsFile || join(__dirname, 'intents.yaml');
    const parsed = yaml.load(readFileSync(path, 'utf8')) as IntentFile;
    this.validate(parsed);
    this.file = parsed;
    this.logger.log(`loaded ${parsed.intents.length} intents (version ${parsed.version}) from ${path}`);
    return parsed;
  }

  all(): IntentDef[] {
    return this.load().intents;
  }

  byId(id: string): IntentDef | undefined {
    return this.all().find((i) => i.id === id);
  }

  fallback(): IntentDef {
    const f = this.byId(this.load().fallback);
    if (!f) {
      throw new Error('intents.yaml: fallback intent missing');
    }
    return f;
  }

  private validate(file: IntentFile): void {
    if (!file || !Array.isArray(file.intents)) {
      throw new Error('intents.yaml: no intents array');
    }
    const seen = new Set<string>();
    for (const i of file.intents) {
      if (!i.id || seen.has(i.id)) {
        throw new Error(`intents.yaml: duplicate or missing id ${i.id}`);
      }
      seen.add(i.id);
      if (!Array.isArray(i.responses) || i.responses.length === 0) {
        throw new Error(`intents.yaml: ${i.id} has no responses`);
      }
    }
    if (!seen.has(file.fallback)) {
      throw new Error(`intents.yaml: fallback ${file.fallback} not defined`);
    }
  }
}
