export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'RELEASED' | 'EXPIRED';

export interface Approval {
  approvalId: string;
  customerId: string;
  organisationId: string;
  kind: 'payment' | 'user-change' | 'limit-change';
  summary: string;
  amountMinor?: number;
  fromAccountId?: string;
  payload: Record<string, unknown>;
  initiatedBy: string;
  initiatedAt: string;
  requiredApprovals: number;
  approvals: Array<{ by: string; at: string }>;
  rejection?: { by: string; at: string; reason: string };
  status: ApprovalStatus;
  expiresAt: string;
}
