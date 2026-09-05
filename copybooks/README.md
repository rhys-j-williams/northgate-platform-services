# Bedrock copybooks

Bedrock is the system of record. It has no JSON interface. Everything Meridian's digital estate
knows about a balance arrives as a fixed width record described by one of the copybooks here,
delivered over IBM MQ and translated by `bedrock-adapter-service`.

| Copybook | Record | Length | Bedrock job | MQ queue |
| --- | --- | --- | --- | --- |
| `MTBACCT.cpy` | Account master extract | 136 | `MTBD140A` | `MTB.BEDROCK.ACCT.OUT` |
| `MTBTRAN.cpy` | Posted transaction | 160 | `MTBD210P` | `MTB.BEDROCK.TRAN.OUT` |
| `MTBCUST.cpy` | Customer party record | 200 | `MTBD100C` | `MTB.BEDROCK.CUST.OUT` |

## Signed zoned decimal

Amount fields are `PIC S9(n) DISPLAY SIGN TRAILING INCLUDED`. The sign travels in the last byte
rather than in a separate position, so `-1234` in a ten position field is `000000123M`, not
`-000001234`. The overpunch table:

| Digit | Positive | Negative |
| --- | --- | --- |
| 0 | `{` | `}` |
| 1 | `A` | `J` |
| 2 | `B` | `K` |
| 3 | `C` | `L` |
| 4 | `D` | `M` |
| 5 | `E` | `N` |
| 6 | `F` | `O` |
| 7 | `G` | `P` |
| 8 | `H` | `Q` |
| 9 | `I` | `R` |

`@meridian/domain-fixtures` implements this in `src/bedrock.ts` and the encoder is unit tested
against the examples above. Do not write another implementation. Two independent decoders is how
INC0044182 happened: the statements batch treated a spaces `TRAN-SETTLED-DATE` as `00000000` and
posted 4,118 statements dated in the year zero.

## Amounts are minor units

Bedrock has no decimal point. `ACCT-CURRENT-BAL` of `000000364085{` is $3,640.85. Nothing in the
estate is allowed to convert this to a floating point number; the TypeScript side keeps
`currentBalanceMinor` as an integer and the Java side uses `long` with `BigDecimal` only at the
presentation boundary.

## Changing a copybook

Copybooks are owned by the Core Banking Services team, not by digital. A change needs a Bedrock
change record, a parallel run and a CAB slot; digital teams cannot widen a field on their own.
Raise a `PLAT` ticket and expect a six week lead time. The practical consequence for the digital
estate is that when a field is too small, the workaround lives in `bedrock-adapter-service`.
