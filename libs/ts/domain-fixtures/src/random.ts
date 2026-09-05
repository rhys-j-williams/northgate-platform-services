/**
 * Deterministic pseudo random source.
 *
 * Every fixture in the estate derives from a seed so that two services asked for the same customer
 * produce the same customer, and so that documentation screenshots do not drift between runs.
 * Mulberry32 is used rather than anything from npm because this package is consumed by an Angular
 * 12 build, an Angular 14 build and Node 14, and it must not drag transitive dependencies along.
 */
export class SeededRandom {
  private state: number;

  constructor(seed: number | string) {
    this.state = typeof seed === 'number' ? seed >>> 0 : SeededRandom.hash(seed);
  }

  /** FNV-1a, so that a string seed such as a customer id gives a stable stream. */
  static hash(value: string): number {
    let hash = 0x811c9dc5;
    for (let index = 0; index < value.length; index++) {
      hash ^= value.charCodeAt(index);
      hash = Math.imul(hash, 0x01000193);
    }
    return hash >>> 0;
  }

  next(): number {
    this.state = (this.state + 0x6d2b79f5) >>> 0;
    let t = this.state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  }

  int(minInclusive: number, maxInclusive: number): number {
    return minInclusive + Math.floor(this.next() * (maxInclusive - minInclusive + 1));
  }

  /** Amount in minor units, so nothing in the estate ever does floating point money. */
  minorUnits(minMajor: number, maxMajor: number): number {
    return this.int(minMajor * 100, maxMajor * 100);
  }

  pick<T>(items: readonly T[]): T {
    if (items.length === 0) {
      throw new Error('cannot pick from an empty list');
    }
    return items[this.int(0, items.length - 1)];
  }

  shuffle<T>(items: readonly T[]): T[] {
    const copy = items.slice();
    for (let index = copy.length - 1; index > 0; index--) {
      const swap = this.int(0, index);
      [copy[index], copy[swap]] = [copy[swap], copy[index]];
    }
    return copy;
  }

  bool(trueProbability = 0.5): boolean {
    return this.next() < trueProbability;
  }

  digits(count: number): string {
    let out = '';
    for (let index = 0; index < count; index++) {
      out += String(this.int(0, 9));
    }
    return out;
  }
}
