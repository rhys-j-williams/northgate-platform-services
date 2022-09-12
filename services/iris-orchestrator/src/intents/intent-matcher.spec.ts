import { IntentLoader } from './intent-loader';
import { IntentMatcher, normalise } from './intent-matcher';

describe('IntentMatcher', () => {
  const matcher = new IntentMatcher(new IntentLoader());

  it.each([
    ['hello there', 'greeting'],
    ["what's my balance", 'balance'],
    ['how much money do i have', 'balance'],
    ['show me recent transactions', 'transactions'],
    ['i did not make this charge', 'dispute'],
    ['why was i charged a fee', 'fees'],
    ['I lost my card', 'card_lost'],
    ['talk to a person please', 'human'],
    ['thanks', 'thanks'],
  ])('%s -> %s', (utterance, expected) => {
    expect(matcher.match(utterance).intent.id).toBe(expected);
  });

  it('falls back on gibberish with zero confidence', () => {
    const m = matcher.match('xylophone quantum');
    expect(m.intent.id).toBe('fallback');
    expect(m.confidence).toBe(0);
  });

  it('dispute outranks fees when both are present', () => {
    // "charged" is a fees keyword, "not mine" is a dispute one; dispute carries weight 1.5
    expect(matcher.match('I was charged for something that is not mine').intent.id).toBe('dispute');
  });

  it('extracts last4 for transactions', () => {
    const m = matcher.match('transactions on 4471');
    expect(m.intent.id).toBe('transactions');
    expect(m.entities).toEqual({ last4: '4471' });
  });

  it('normalises punctuation and case', () => {
    expect(normalise("  What's   MY balance?! ")).toBe("what's my balance");
  });
});
