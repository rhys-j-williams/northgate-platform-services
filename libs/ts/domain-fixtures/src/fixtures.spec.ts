import { decodeAccountRecord, decodeZonedDecimal, encodeAccountRecord, encodeTransactionRecord,
  encodeZonedDecimal } from './bedrock';
import { generateFixtures } from './generators';
import { luhnIsValid, maskAccountNumber, TEST_ROUTING_NUMBER } from './safety';
import { SeededRandom } from './random';

describe('generateFixtures', () => {
  const fixtures = generateFixtures({ seed: 'spec', customers: 12 });

  it('is deterministic for a given seed', () => {
    const again = generateFixtures({ seed: 'spec', customers: 12 });
    expect(again.customers).toEqual(fixtures.customers);
    expect(again.transactions.length).toBe(fixtures.transactions.length);
  });

  it('gives every customer at least two accounts', () => {
    for (const customer of fixtures.customers) {
      const owned = fixtures.accounts.filter((a) => a.customerId === customer.customerId);
      expect(owned.length).toBeGreaterThanOrEqual(2);
    }
  });

  it('produces transactions only for accounts that exist', () => {
    const ids = new Set(fixtures.accounts.map((a) => a.accountId));
    for (const transaction of fixtures.transactions) {
      expect(ids.has(transaction.accountId)).toBe(true);
    }
  });

  it('gives consumers no entitlements and organisations a single administrator', () => {
    for (const customer of fixtures.customers) {
      const owned = fixtures.entitlements.filter((e) => e.customerId === customer.customerId);
      if (customer.segment === 'consumer') {
        expect(owned).toHaveLength(0);
      } else {
        expect(owned.filter((e) => e.role === 'administrator')).toHaveLength(1);
      }
    }
  });
});

describe('data safety guarantees', () => {
  const fixtures = generateFixtures({ seed: 'safety', customers: 30 });

  it('never generates a card number that passes the Luhn check', () => {
    expect(fixtures.cards.length).toBeGreaterThan(0);
    for (const card of fixtures.cards) {
      expect(luhnIsValid(card.cardNumber)).toBe(false);
    }
  });

  it('only uses the reserved test routing number', () => {
    for (const account of fixtures.accounts) {
      expect(account.routingNumber).toBe(TEST_ROUTING_NUMBER);
    }
    for (const payee of fixtures.payees) {
      expect(payee.routingNumber).toBe(TEST_ROUTING_NUMBER);
    }
  });

  it('uses example.com for every generated email address', () => {
    for (const customer of fixtures.customers) {
      expect(customer.email.endsWith('@example.com')).toBe(true);
    }
  });

  it('never leaves a regulatory alert disabled', () => {
    for (const preference of fixtures.alertPreferences) {
      if (preference.regulatory) {
        expect(preference.enabled).toBe(true);
      }
    }
  });

  it('masks an account number down to the last four digits', () => {
    expect(maskAccountNumber('4820019374')).toBe('••••9374');
  });
});

describe('Bedrock zoned decimal', () => {
  it('round trips a positive amount', () => {
    expect(encodeZonedDecimal(1234, 10)).toBe('000000123D');
    expect(decodeZonedDecimal('000000123D')).toBe(1234);
  });

  it('round trips a negative amount', () => {
    expect(encodeZonedDecimal(-1234, 10)).toBe('000000123M');
    expect(decodeZonedDecimal('000000123M')).toBe(-1234);
  });

  it('round trips zero as a positive value', () => {
    expect(encodeZonedDecimal(0, 6)).toBe('00000{');
    expect(decodeZonedDecimal('00000{')).toBe(0);
  });

  it('rejects a field that is not zoned decimal', () => {
    expect(() => decodeZonedDecimal('0000012')).toThrow(/not a zoned decimal/);
  });
});

describe('Bedrock records', () => {
  const fixtures = generateFixtures({ seed: 'bedrock', customers: 4 });
  const account = fixtures.accounts[0];
  const customer = fixtures.customers.find((c) => c.customerId === account.customerId)!;

  it('writes MTBACCT at exactly 136 positions and reads it back', () => {
    const record = encodeAccountRecord(account, customer);
    expect(record).toHaveLength(136);
    const decoded = decodeAccountRecord(record);
    expect(decoded.accountId).toBe(account.accountId);
    expect(decoded.currentBalanceMinor).toBe(account.currentBalanceMinor);
    expect(decoded.availableBalanceMinor).toBe(account.availableBalanceMinor);
    expect(decoded.routingNumber).toBe(TEST_ROUTING_NUMBER);
  });

  it('writes MTBTRAN at exactly 160 positions', () => {
    const transaction = fixtures.transactions.find((t) => t.accountId === account.accountId)!;
    expect(encodeTransactionRecord(transaction)).toHaveLength(160);
  });

  it('refuses a short MTBACCT record', () => {
    expect(() => decodeAccountRecord('too short')).toThrow(/136 positions/);
  });
});

describe('SeededRandom', () => {
  it('produces the same stream for the same string seed', () => {
    const left = new SeededRandom('CUS-100001');
    const right = new SeededRandom('CUS-100001');
    expect([left.next(), left.next()]).toEqual([right.next(), right.next()]);
  });

  it('stays within the requested integer bounds', () => {
    const random = new SeededRandom(7);
    for (let index = 0; index < 500; index++) {
      const value = random.int(3, 9);
      expect(value).toBeGreaterThanOrEqual(3);
      expect(value).toBeLessThanOrEqual(9);
    }
  });
});
