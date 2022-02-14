import { FixtureSet, generateFixtures } from '@meridian/domain-fixtures';
import { config } from './config';

let set: FixtureSet | undefined;

export function fixtures(): FixtureSet {
  if (!set) {
    set = generateFixtures({ seed: config.fixtureSeed });
  }
  return set;
}
