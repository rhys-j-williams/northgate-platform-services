/** What retail-web renders. Account numbers are masked here and nowhere else (DATA_CLASSIFICATION.md). */
export interface AccountSummaryDto {
  accountId: string;
  type: string;
  nickname: string;
  maskedNumber: string;
  currentBalance: Money;
  availableBalance: Money;
  status: string;
  openedOn: string;
}

export interface Money {
  amount: string;
  currency: 'USD';
  minor: number;
}

export interface TransactionDto {
  transactionId: string;
  accountId: string;
  postedOn: string;
  settledOn: string | null;
  description: string;
  merchantCategoryCode: string;
  category: string;
  amount: Money;
  runningBalance: Money;
  status: string;
  channel: string;
  pending: boolean;
}

export interface DashboardDto {
  customerId: string;
  displayName: string;
  accounts: AccountSummaryDto[];
  totals: { deposits: Money; borrowing: Money };
  asOf: string;
}
