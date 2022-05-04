import { Injectable } from '@nestjs/common';
import { FixtureSet, generateFixtures } from '@meridian/domain-fixtures';
import { config } from '../config';

/**
 * Local fallback data. Same seed as scripts/export-fixtures.js so the Java services and this BFF
 * agree on ids and balances when the mocks are not running. Generation takes ~400ms for 25
 * customers so it is lazy and done once.
 */
@Injectable()
export class FixturesService {
  private set: FixtureSet | undefined;

  get(): FixtureSet {
    if (!this.set) {
      this.set = generateFixtures({ seed: config.fixtureSeed });
    }
    return this.set;
  }
}
