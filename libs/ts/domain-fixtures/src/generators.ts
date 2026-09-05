import { SeededRandom } from './random';
import { makeLuhnInvalid, TEST_ROUTING_NUMBER } from './safety';
import {
  Account, AccountType, AlertPreference, Card, Channel, Customer, Entitlement, FixtureSet, Payee,
  Transaction, TransactionCategory
} from './types';
import {
  ALERT_CATALOGUE, CITIES, FAMILY_NAMES, GIVEN_NAMES, MERCHANTS, ORGANISATION_NAMES, PAYEE_NAMES,
  STREETS, TREASURY_ORGANISATIONS
} from './vocabulary';

export interface GenerateOptions {
  seed?: number | string;
  customers?: number;
  /** Bias the population. Defaults to the retail heavy mix the demo uses. */
  segmentMix?: { consumer: number; smallBusiness: number; treasury: number };
  /** How far back transaction history runs. */
  monthsOfHistory?: number;
  /** Fixed "today" so that generated dates never move under a screenshot. */
  asOf?: string;
}

const DEFAULTS: Required<GenerateOptions> = {
  seed: 'meridian',
  customers: 25,
  segmentMix: { consumer: 0.7, smallBusiness: 0.22, treasury: 0.08 },
  monthsOfHistory: 18,
  asOf: '2024-11-15T00:00:00.000Z'
};

const CONSUMER_ACCOUNTS: AccountType[] = ['checking', 'savings', 'credit-card', 'mortgage',
  'auto-loan', 'certificate'];
const BUSINESS_ACCOUNTS: AccountType[] = ['business-checking', 'business-savings', 'credit-card'];
const TREASURY_ACCOUNTS: AccountType[] = ['treasury-operating', 'business-savings',
  'business-checking'];

const NICKNAMES: Record<AccountType, string[]> = {
  checking: ['Everyday checking', 'Household account', 'Bills account'],
  savings: ['Rainy day', 'Holiday fund', 'Emergency savings'],
  'credit-card': ['Meridian Rewards card', 'Everyday card', 'Travel card'],
  mortgage: ['Home loan'],
  'auto-loan': ['Car loan'],
  certificate: ['18 month certificate', '24 month certificate'],
  'business-checking': ['Operating account', 'Trading account'],
  'business-savings': ['Reserve account', 'Tax set aside'],
  'treasury-operating': ['Group operating account', 'Concentration account']
};

function isoDate(base: Date, dayOffset: number): string {
  const copy = new Date(base.getTime());
  copy.setUTCDate(copy.getUTCDate() + dayOffset);
  return copy.toISOString();
}

function id(prefix: string, random: SeededRandom): string {
  return `${prefix}-${random.digits(9)}`;
}

export function generateCustomer(random: SeededRandom, index: number,
                                 segment: Customer['segment'], asOf: Date): Customer {
  const firstName = random.pick(GIVEN_NAMES);
  const lastName = random.pick(FAMILY_NAMES);
  const [city, state, postalCode] = random.pick(CITIES);
  const customerId = `CUS-${String(100000 + index)}`;
  const organisationName = segment === 'treasury'
    ? random.pick(TREASURY_ORGANISATIONS)
    : segment === 'small-business' ? random.pick(ORGANISATION_NAMES) : undefined;

  return {
    customerId,
    segment,
    firstName,
    lastName,
    displayName: `${firstName} ${lastName}`,
    email: `${firstName}.${lastName}`.toLowerCase().replace(/[^a-z.]/g, '') + '@example.com',
    mobile: `+1555${random.digits(7)}`,
    address: {
      line1: `${random.int(1, 480)} ${random.pick(STREETS)}`,
      city,
      state,
      postalCode,
      country: 'US'
    },
    enrolledAt: isoDate(asOf, -random.int(400, 3200)),
    organisationName,
    taxIdLastFour: organisationName ? random.digits(4) : undefined
  };
}

export function generateAccounts(random: SeededRandom, customer: Customer, asOf: Date): Account[] {
  const pool = customer.segment === 'consumer' ? CONSUMER_ACCOUNTS
    : customer.segment === 'small-business' ? BUSINESS_ACCOUNTS : TREASURY_ACCOUNTS;
  const count = customer.segment === 'consumer' ? random.int(2, 5) : random.int(2, 3);
  const types = random.shuffle(pool).slice(0, count);

  return types.map((type) => {
    const isCredit = type === 'credit-card' || type === 'mortgage' || type === 'auto-loan';
    const current = isCredit
      ? -random.minorUnits(240, type === 'mortgage' ? 384000 : 24000)
      : random.minorUnits(120, customer.segment === 'treasury' ? 4800000 : 42000);

    return {
      accountId: id('ACC', random),
      customerId: customer.customerId,
      type,
      nickname: random.pick(NICKNAMES[type]),
      accountNumber: random.digits(10),
      routingNumber: TEST_ROUTING_NUMBER,
      currency: 'USD' as const,
      currentBalanceMinor: current,
      availableBalanceMinor: isCredit ? current : Math.max(0, current - random.minorUnits(0, 200)),
      openedAt: isoDate(asOf, -random.int(200, 3000)),
      status: random.bool(0.94) ? 'open' as const : random.pick(['dormant', 'restricted'] as const),
      interestRateBasisPoints: type === 'savings' || type === 'certificate'
        ? random.int(85, 465) : undefined,
      creditLimitMinor: type === 'credit-card' ? random.minorUnits(1500, 30000) : undefined
    };
  });
}

export function generateCards(random: SeededRandom, customer: Customer,
                              accounts: Account[]): Card[] {
  const eligible = accounts.filter((account) =>
    account.type === 'checking' || account.type === 'business-checking'
    || account.type === 'credit-card');

  return eligible.map((account) => {
    const network = account.type === 'credit-card' ? 'meridian-credit' as const
      : 'meridian-debit' as const;
    const raw = `4${random.digits(15)}`;
    return {
      cardId: id('CRD', random),
      customerId: customer.customerId,
      accountId: account.accountId,
      cardNumber: makeLuhnInvalid(raw),
      network,
      expiryMonth: random.int(1, 12),
      expiryYear: random.int(2026, 2030),
      status: random.bool(0.9) ? 'active' as const : random.pick(['locked', 'replaced'] as const),
      contactlessEnabled: random.bool(0.85),
      digitalWallet: {
        applePay: random.bool(0.6),
        googlePay: random.bool(0.45),
        samsungPay: random.bool(0.15)
      }
    };
  });
}

export function generateTransactions(random: SeededRandom, account: Account, asOf: Date,
                                     months: number): Transaction[] {
  if (account.type === 'mortgage' || account.type === 'certificate') {
    return generateScheduledTransactions(random, account, asOf, months);
  }

  const perMonth = account.type.startsWith('business') || account.type === 'treasury-operating'
    ? random.int(45, 90) : random.int(18, 42);
  const total = perMonth * months;
  const transactions: Transaction[] = [];
  let running = account.currentBalanceMinor;

  for (let index = 0; index < total; index++) {
    const merchant = random.pick(MERCHANTS);
    const dayOffset = -Math.floor((index / total) * months * 30) - random.int(0, 2);
    const isIncome = random.bool(account.type === 'credit-card' ? 0.04 : 0.09);
    const amount = isIncome
      ? random.minorUnits(400, 4200)
      : -random.minorUnits(merchant.low, merchant.high);
    const pending = dayOffset > -3 && random.bool(0.35);

    transactions.push({
      transactionId: id('TXN', random),
      accountId: account.accountId,
      postedAt: isoDate(asOf, dayOffset),
      settledAt: pending ? null : isoDate(asOf, dayOffset + 1),
      description: isIncome ? 'Direct deposit — payroll' : merchant.name.toUpperCase(),
      merchantName: isIncome ? 'Payroll' : merchant.name,
      merchantCategoryCode: isIncome ? '0000' : merchant.mcc,
      category: (isIncome ? 'income' : merchant.category) as TransactionCategory,
      amountMinor: amount,
      runningBalanceMinor: running,
      status: pending ? 'pending' : random.bool(0.985) ? 'posted'
        : random.pick(['disputed', 'reversed'] as const),
      channel: isIncome ? 'ach' : random.pick(['card', 'card', 'card', 'ach', 'paylink',
        'atm'] as const)
    });
    running -= amount;
  }

  return transactions.sort((left, right) => right.postedAt.localeCompare(left.postedAt));
}

function generateScheduledTransactions(random: SeededRandom, account: Account, asOf: Date,
                                       months: number): Transaction[] {
  const payment = random.minorUnits(420, 2600);
  const out: Transaction[] = [];
  for (let month = 0; month < months; month++) {
    out.push({
      transactionId: id('TXN', random),
      accountId: account.accountId,
      postedAt: isoDate(asOf, -month * 30),
      settledAt: isoDate(asOf, -month * 30 + 1),
      description: account.type === 'mortgage' ? 'Scheduled mortgage payment'
        : 'Certificate interest credit',
      merchantName: 'Meridian Trust Bank',
      merchantCategoryCode: '6012',
      category: account.type === 'mortgage' ? 'transfers' : 'income',
      amountMinor: account.type === 'mortgage' ? payment : Math.round(payment / 12),
      runningBalanceMinor: account.currentBalanceMinor + month * payment,
      status: 'posted',
      channel: 'internal'
    });
  }
  return out;
}

export function generatePayees(random: SeededRandom, customer: Customer, asOf: Date): Payee[] {
  const count = random.int(2, 7);
  return random.shuffle(PAYEE_NAMES).slice(0, count).map((name) => ({
    payeeId: id('PYE', random),
    customerId: customer.customerId,
    name,
    nickname: name.split(' ')[0],
    accountNumberLastFour: random.digits(4),
    routingNumber: TEST_ROUTING_NUMBER,
    type: random.pick(['bill-pay', 'external-transfer', 'paylink'] as const),
    verified: random.bool(0.8),
    addedAt: isoDate(asOf, -random.int(20, 1400))
  }));
}

export function generateAlertPreferences(random: SeededRandom,
                                         customer: Customer): AlertPreference[] {
  return ALERT_CATALOGUE.map((entry) => {
    const channels: Channel[] = random.shuffle(['push', 'sms', 'email', 'in-app'] as Channel[])
      .slice(0, random.int(1, 3));
    return {
      alertId: `${customer.customerId}-${entry.code}`,
      customerId: customer.customerId,
      code: entry.code,
      label: entry.label,
      description: entry.description,
      regulatory: entry.regulatory,
      // A regulatory alert is always on. alerts-preferences-service rejects an attempt to
      // disable one; the fixture must never present an off regulatory alert to begin with.
      enabled: entry.regulatory ? true : random.bool(0.62),
      channels: entry.regulatory ? ['email', 'in-app'] : channels,
      thresholdMinor: entry.code === 'BALANCE_LOW' || entry.code === 'LARGE_TRANSACTION'
        ? random.minorUnits(50, 2500) : undefined,
      quietHours: !entry.regulatory && random.bool(0.3)
        ? { start: '22:00', end: '07:00' } : undefined
    };
  });
}

export function generateEntitlements(random: SeededRandom, customer: Customer): Entitlement[] {
  if (customer.segment === 'consumer') {
    return [];
  }
  const organisationId = id('ORG', random);
  const roles: Entitlement['role'][] = customer.segment === 'treasury'
    ? ['administrator', 'approver', 'approver', 'initiator', 'initiator', 'viewer', 'auditor']
    : ['administrator', 'initiator', 'viewer'];

  return roles.map((role, index) => ({
    entitlementId: id('ENT', random),
    customerId: customer.customerId,
    organisationId,
    userHandle: `${random.pick(GIVEN_NAMES)}.${random.pick(FAMILY_NAMES)}`.toLowerCase()
      + `.${index}`,
    role,
    permissions: permissionsFor(role),
    dualApprovalRequired: role === 'initiator' && customer.segment === 'treasury',
    limitPerTransactionMinor: role === 'initiator' ? random.minorUnits(1000, 250000) : undefined,
    limitPerDayMinor: role === 'initiator' ? random.minorUnits(250000, 2000000) : undefined
  }));
}

function permissionsFor(role: Entitlement['role']): string[] {
  switch (role) {
    case 'administrator':
      return ['users:manage', 'accounts:view', 'payments:initiate', 'payments:approve',
        'reports:run', 'entitlements:manage'];
    case 'approver':
      return ['accounts:view', 'payments:approve', 'reports:run'];
    case 'initiator':
      return ['accounts:view', 'payments:initiate', 'reports:run'];
    case 'auditor':
      return ['accounts:view', 'reports:run', 'audit:read'];
    default:
      return ['accounts:view'];
  }
}

export function generateFixtures(options: GenerateOptions = {}): FixtureSet {
  const settings = { ...DEFAULTS, ...options };
  const random = new SeededRandom(settings.seed);
  const asOf = new Date(settings.asOf);

  const customers: Customer[] = [];
  const accounts: Account[] = [];
  const cards: Card[] = [];
  const transactions: Transaction[] = [];
  const payees: Payee[] = [];
  const alertPreferences: AlertPreference[] = [];
  const entitlements: Entitlement[] = [];

  for (let index = 0; index < settings.customers; index++) {
    const roll = random.next();
    const segment: Customer['segment'] = roll < settings.segmentMix.consumer ? 'consumer'
      : roll < settings.segmentMix.consumer + settings.segmentMix.smallBusiness ? 'small-business'
        : 'treasury';

    const customer = generateCustomer(random, index, segment, asOf);
    const customerAccounts = generateAccounts(random, customer, asOf);

    customers.push(customer);
    accounts.push(...customerAccounts);
    cards.push(...generateCards(random, customer, customerAccounts));
    payees.push(...generatePayees(random, customer, asOf));
    alertPreferences.push(...generateAlertPreferences(random, customer));
    entitlements.push(...generateEntitlements(random, customer));

    for (const account of customerAccounts) {
      transactions.push(...generateTransactions(random, account, asOf, settings.monthsOfHistory));
    }
  }

  return {
    seed: settings.seed, customers, accounts, cards, transactions, payees, alertPreferences,
    entitlements
  };
}
