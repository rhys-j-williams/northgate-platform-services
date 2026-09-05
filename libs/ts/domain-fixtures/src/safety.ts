/**
 * Guarantees that make this data safe to commit, screenshot and hand to a vendor.
 *
 * DATA_CLASSIFICATION.md in every repository points at these functions. They are exported so that
 * a test in any consuming service can assert them, and the estate verification script does exactly
 * that.
 */

/** Reserved test routing number. Not issued to any institution. */
export const TEST_ROUTING_NUMBER = '021000000';

/** Luhn check digit calculation, used here only to prove that a number fails it. */
export function luhnIsValid(cardNumber: string): boolean {
  const digits = cardNumber.replace(/\D/g, '');
  if (digits.length < 12) {
    return false;
  }
  let sum = 0;
  let double = false;
  for (let index = digits.length - 1; index >= 0; index--) {
    let value = Number(digits[index]);
    if (double) {
      value *= 2;
      if (value > 9) {
        value -= 9;
      }
    }
    sum += value;
    double = !double;
  }
  return sum % 10 === 0;
}

/**
 * Force a card number to fail the Luhn check. Called on every generated card. If a generated
 * number happens to be valid, the last digit is nudged, which is enough to break the check without
 * changing the shape of the number.
 */
export function makeLuhnInvalid(cardNumber: string): string {
  if (!luhnIsValid(cardNumber)) {
    return cardNumber;
  }
  const last = Number(cardNumber[cardNumber.length - 1]);
  const replacement = (last + 1) % 10;
  return cardNumber.slice(0, -1) + String(replacement);
}

/** Account numbers are shown to customers as the last four digits only. */
export function maskAccountNumber(accountNumber: string): string {
  const digits = accountNumber.replace(/\D/g, '');
  return `••••${digits.slice(-4)}`;
}

export function maskCardNumber(cardNumber: string): string {
  const digits = cardNumber.replace(/\D/g, '');
  return `•••• •••• •••• ${digits.slice(-4)}`;
}
