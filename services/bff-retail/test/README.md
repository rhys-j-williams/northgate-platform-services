# Tests

Unit suites live beside the code as `*.spec.ts`. There is no application level (supertest) suite
any more: it was removed in PLAT-1961 because it pulled the Nest container up per test file and
pushed the pipeline over the twelve minute agent budget on nodejs18-rhel9. The plan was to bring it
back behind a `test:e2e` script once the mocks were in compose; that never happened. The controllers,
transfers flow, cards, payees, PayLink and the partner clients are therefore only exercised by
smoke.sh in mock-external and by hand.

`setup-env.ts` points every upstream at a closed port so the fixture fallback path is what runs.
