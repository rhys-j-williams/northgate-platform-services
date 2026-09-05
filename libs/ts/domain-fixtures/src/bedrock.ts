/**
 * Bedrock is the core banking system of record. It speaks fixed width records described by the
 * copybooks in `platform-services/copybooks/`. The mock Bedrock and bedrock-adapter-service both
 * use these helpers so that the record layout is defined in exactly one place.
 *
 * Bedrock stores amounts as signed zoned decimal: the sign is carried in the final digit. A
 * balance of -1234 cents is written `000000123M`. Getting this wrong is a classic incident, so the
 * encoder and decoder are unit tested against the copybook examples.
 */

import { Account, Customer, Transaction } from './types';

const NEGATIVE_OVERPUNCH = '}JKLMNOPQR';
const POSITIVE_OVERPUNCH = '{ABCDEFGHI';

export function encodeZonedDecimal(minorUnits: number, width: number): string {
  const negative = minorUnits < 0;
  const digits = String(Math.abs(minorUnits)).padStart(width, '0');
  if (digits.length > width) {
    throw new Error(`amount ${minorUnits} does not fit in ${width} positions`);
  }
  const lead = digits.slice(0, width - 1);
  const last = Number(digits[width - 1]);
  return lead + (negative ? NEGATIVE_OVERPUNCH[last] : POSITIVE_OVERPUNCH[last]);
}

export function decodeZonedDecimal(field: string): number {
  const lead = field.slice(0, -1);
  const sign = field.slice(-1);
  const negativeIndex = NEGATIVE_OVERPUNCH.indexOf(sign);
  const positiveIndex = POSITIVE_OVERPUNCH.indexOf(sign);
  if (negativeIndex === -1 && positiveIndex === -1) {
    throw new Error(`not a zoned decimal field: ${field}`);
  }
  const digit = negativeIndex === -1 ? positiveIndex : negativeIndex;
  const magnitude = Number(lead + String(digit));
  return negativeIndex === -1 ? magnitude : -magnitude;
}

function text(value: string, width: number): string {
  return value.slice(0, width).padEnd(width, ' ');
}

function yyyymmdd(iso: string): string {
  return iso.slice(0, 10).replace(/-/g, '');
}

/** MTBACCT copybook, record length 136. */
export function encodeAccountRecord(account: Account, customer: Customer): string {
  return [
    text(account.accountId, 16),
    text(account.customerId, 12),
    text(account.type.toUpperCase(), 20),
    text(account.accountNumber, 10),
    text(account.routingNumber, 9),
    encodeZonedDecimal(account.currentBalanceMinor, 13),
    encodeZonedDecimal(account.availableBalanceMinor, 13),
    yyyymmdd(account.openedAt),
    text(account.status.toUpperCase(), 10),
    text(customer.displayName, 25)
  ].join('').padEnd(136, ' ');
}

/** MTBTRAN copybook, record length 160. */
export function encodeTransactionRecord(transaction: Transaction): string {
  return [
    text(transaction.transactionId, 16),
    text(transaction.accountId, 16),
    yyyymmdd(transaction.postedAt),
    transaction.settledAt ? yyyymmdd(transaction.settledAt) : '        ',
    encodeZonedDecimal(transaction.amountMinor, 13),
    encodeZonedDecimal(transaction.runningBalanceMinor, 13),
    text(transaction.merchantCategoryCode, 4),
    text(transaction.channel.toUpperCase(), 8),
    text(transaction.status.toUpperCase(), 10),
    text(transaction.description, 64)
  ].join('').padEnd(160, ' ');
}

export interface DecodedAccountRecord {
  accountId: string;
  customerId: string;
  type: string;
  accountNumber: string;
  routingNumber: string;
  currentBalanceMinor: number;
  availableBalanceMinor: number;
  openedOn: string;
  status: string;
  ownerName: string;
}

export function decodeAccountRecord(record: string): DecodedAccountRecord {
  if (record.length < 136) {
    throw new Error(`MTBACCT record must be 136 positions, received ${record.length}`);
  }
  const slice = (start: number, width: number) => record.substr(start, width).trim();
  return {
    accountId: slice(0, 16),
    customerId: slice(16, 12),
    type: slice(28, 20),
    accountNumber: slice(48, 10),
    routingNumber: slice(58, 9),
    currentBalanceMinor: decodeZonedDecimal(record.substr(67, 13)),
    availableBalanceMinor: decodeZonedDecimal(record.substr(80, 13)),
    openedOn: slice(93, 8),
    status: slice(101, 10),
    ownerName: slice(111, 25)
  };
}
