# @meridian/domain-fixtures

Seeded synthetic banking data. Every mock, backend for frontend and front end test suite in the
estate gets its customers, accounts, cards, transactions, payees, alert preferences and
entitlements from here, so that a customer id means the same thing in retail-web, in the Bedrock
mock and in a Cypress run.

Published to the internal registry (Verdaccio at `http://localhost:4873` when the estate is up).

```bash
npm install @meridian/domain-fixtures
```

```ts
import { generateFixtures, maskAccountNumber } from '@meridian/domain-fixtures';

const estate = generateFixtures({ seed: 'retail-web-e2e', customers: 40 });
const [customer] = estate.customers;
const accounts = estate.accounts.filter((a) => a.customerId === customer.customerId);

console.log(customer.displayName, accounts.map((a) => maskAccountNumber(a.accountNumber)));
```

## Determinism

The same seed always produces the same estate. Seeds are strings so a suite can namespace its own
data: `generateFixtures({ seed: 'CNPY-visual-regression' })` will never collide with the BFF's
`generateFixtures({ seed: 'bff-retail' })`. Nothing here reads the clock; `asOf` defaults to a
fixed date so a screenshot taken today matches one taken next quarter.

## Data safety

This package is why estate data is safe to commit and to screenshot. It is enforced by tests, not
by convention:

- every generated card number **fails** the Luhn check, so nothing generated here can be presented
  to a real payment network
- every account and payee carries `021000000`, a reserved test routing number issued to no
  institution
- every customer email is `@example.com`
- names, merchants and organisations are invented; `src/vocabulary.ts` is the whole list
- addresses are real cities with invented streets

`src/fixtures.spec.ts` asserts all of the above. `scripts/verify-estate.sh` runs that suite as part
of the estate check. If you add a generator, add the matching guarantee test.

## Bedrock records

`src/bedrock.ts` encodes and decodes the fixed width records described by
`platform-services/copybooks/`, including signed zoned decimal overpunch. It is the single
implementation in the estate; the Java side calls the adapter, not its own decoder.

## Layout

| File | Contents |
| --- | --- |
| `src/random.ts` | Mulberry32 seeded source, no dependencies so Node 14 can use it |
| `src/safety.ts` | Luhn, masking, reserved routing number |
| `src/types.ts` | The domain types every consumer imports |
| `src/vocabulary.ts` | Invented names, merchants, alert catalogue |
| `src/generators.ts` | Population generation |
| `src/bedrock.ts` | Copybook encode and decode |

## Owning team

Platform Engineering. Changes that alter generated shapes affect every consumer, so they go through
the `PLAT` board and need the consuming teams on the pull request.
