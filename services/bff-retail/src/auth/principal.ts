export interface Principal {
  subject: string;
  customerId: string;
  segment: 'consumer' | 'small-business' | 'treasury';
  scopes: string[];
  /** Epoch seconds of the last MFA step up, from Keystone's `mfa_at` claim. retail-web's MfaStepUpGuard mirrors the 10 minute rule. */
  mfaAt?: number;
  sessionId?: string;
}

export const PRINCIPAL_KEY = 'meridian.principal';
