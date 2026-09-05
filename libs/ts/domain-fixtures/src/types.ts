export type AccountType = 'checking' | 'savings' | 'credit-card' | 'mortgage' | 'auto-loan'
  | 'certificate' | 'business-checking' | 'business-savings' | 'treasury-operating';

export type Channel = 'push' | 'sms' | 'email' | 'in-app';

export interface Address {
  line1: string;
  line2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: 'US';
}

export interface Customer {
  customerId: string;
  /** Party segment. Drives which applications the customer can see. */
  segment: 'consumer' | 'small-business' | 'treasury';
  firstName: string;
  lastName: string;
  displayName: string;
  email: string;
  mobile: string;
  address: Address;
  enrolledAt: string;
  /** Present for small business and treasury customers. */
  organisationName?: string;
  taxIdLastFour?: string;
}

export interface Account {
  accountId: string;
  customerId: string;
  type: AccountType;
  nickname: string;
  /** Full number. Never rendered to a screen; use maskAccountNumber. */
  accountNumber: string;
  routingNumber: string;
  currency: 'USD';
  /** Minor units. Negative for credit balances owed. */
  currentBalanceMinor: number;
  availableBalanceMinor: number;
  openedAt: string;
  status: 'open' | 'dormant' | 'restricted' | 'closed';
  interestRateBasisPoints?: number;
  creditLimitMinor?: number;
}

export interface Card {
  cardId: string;
  customerId: string;
  accountId: string;
  /** Deliberately fails the Luhn check. See safety.ts. */
  cardNumber: string;
  network: 'meridian-debit' | 'meridian-credit';
  expiryMonth: number;
  expiryYear: number;
  status: 'active' | 'locked' | 'replaced' | 'expired';
  contactlessEnabled: boolean;
  travelNoticeUntil?: string;
  digitalWallet: { applePay: boolean; googlePay: boolean; samsungPay: boolean };
}

export type TransactionCategory = 'groceries' | 'dining' | 'fuel' | 'travel' | 'utilities'
  | 'healthcare' | 'entertainment' | 'transfers' | 'income' | 'fees' | 'insurance'
  | 'home-improvement' | 'education' | 'charity' | 'payroll' | 'taxes';

export interface Transaction {
  transactionId: string;
  accountId: string;
  postedAt: string;
  /** Null while the transaction is still authorised but not posted. */
  settledAt: string | null;
  description: string;
  merchantName: string;
  merchantCategoryCode: string;
  category: TransactionCategory;
  amountMinor: number;
  runningBalanceMinor: number;
  status: 'pending' | 'posted' | 'disputed' | 'reversed';
  channel: 'card' | 'ach' | 'wire' | 'internal' | 'paylink' | 'check' | 'atm' | 'fee';
  disputeId?: string;
}

export interface Payee {
  payeeId: string;
  customerId: string;
  name: string;
  nickname: string;
  accountNumberLastFour: string;
  routingNumber: string;
  type: 'bill-pay' | 'external-transfer' | 'paylink';
  verified: boolean;
  addedAt: string;
}

export interface AlertPreference {
  alertId: string;
  customerId: string;
  code: string;
  label: string;
  description: string;
  /** Regulatory alerts cannot be switched off. alerts-preferences-service enforces this. */
  regulatory: boolean;
  enabled: boolean;
  channels: Channel[];
  thresholdMinor?: number;
  quietHours?: { start: string; end: string };
}

export interface Entitlement {
  entitlementId: string;
  customerId: string;
  organisationId: string;
  userHandle: string;
  role: 'administrator' | 'approver' | 'initiator' | 'viewer' | 'auditor';
  permissions: string[];
  dualApprovalRequired: boolean;
  limitPerTransactionMinor?: number;
  limitPerDayMinor?: number;
}

export interface FixtureSet {
  seed: number | string;
  customers: Customer[];
  accounts: Account[];
  cards: Card[];
  transactions: Transaction[];
  payees: Payee[];
  alertPreferences: AlertPreference[];
  entitlements: Entitlement[];
}
