import { Money } from './account.dto';

export function money(minor: number): Money {
  const sign = minor < 0 ? '-' : '';
  const abs = Math.abs(minor);
  return { minor, currency: 'USD', amount: `${sign}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}` };
}

/** MCC to the category vocabulary retail-web's spending chart groups on. Partial on purpose; unknown codes fall to 'other'. */
const MCC_CATEGORIES: Record<string, string> = {
  '5411': 'groceries',
  '5812': 'dining',
  '5814': 'dining',
  '5541': 'fuel',
  '4511': 'travel',
  '7011': 'travel',
  '4900': 'utilities',
  '8062': 'healthcare',
  '5912': 'healthcare',
  '7832': 'entertainment',
  '4829': 'transfers',
  '6012': 'fees',
  '6300': 'insurance',
  '5200': 'home-improvement',
  '8220': 'education',
  '8398': 'charity',
  '0000': 'income',
};

export function categoryForMcc(mcc: string): string {
  return MCC_CATEGORIES[mcc] ?? 'other';
}
