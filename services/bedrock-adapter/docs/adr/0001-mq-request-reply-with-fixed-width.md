# ADR 0001: MQ request-reply with fixed width records rather than a REST facade on the mainframe

Status: Accepted
Owner: payments-platform

## Context

The Bedrock team offers two integration paths: the CICS Transaction Gateway with COMMAREA records
over MQ, or the z/OS Connect REST facade which was pilot-only in 2020 and priced per API. The
retail programme needed balances and transactions in Q1 2021.

## Decision

Use MQ request-reply against the existing CICS transactions (ACCTINQ, TXNPOST, CUSTPROF), encoding
and decoding the COMMAREA with the copybooks checked into this repository. A single `FixedWidthCodec`
owns the byte layout. The overpunch algorithm is shared with the TypeScript fixtures package so the
front end teams' test data is byte compatible with what the adapter emits.

Locally, IBM MQ is replaced by embedded Artemis behind the `local-artemis` profile. Same queue
names, same JmsTemplate, different ConnectionFactory bean. No code path may check which one it is
talking to.

## Consequences

* Every layout change on the mainframe side is a coordinated release. We have been bitten (INC0091132).
* z/OS Connect became generally available in 2023 and the architecture forum wants us on it by 2027 (ARCH-0412). This ADR is superseded-in-principle but nothing has moved.
* Testing the byte layout is cheap and we do it. Testing MQ behaviour is not and we mostly do not, which is why coverage sits where it does.
