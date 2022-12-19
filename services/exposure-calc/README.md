# exposure-calc

Position and exposure calculator for the treasury desk. Port 4520. Python 3.11, FastAPI, numpy.

Owner: Treasury Technology (the two of them). No on-call rota; if it is down the desk uses the
spreadsheet and raises a TRS ticket in the morning.

## What it does

- `PUT /exposure/v1/books/{book}/positions` loads a book (`USD-RATES`, `FX-G10`, `LIQUIDITY`).
- `GET /exposure/v1/books/{book}/exposure` gross/net notional, DV01, by currency, limit utilisation.
- `GET /exposure/v1/books/{book}/var?confidence=0.99&horizonDays=1` parametric VaR, no correlation.
- `POST /exposure/v1/books/{book}/scenario` rates / FX / spread shock P&L.

Everything is in memory. Restart and it is gone, except the seeded demo books.

## Run

    ./run.sh

That is the whole deployment story. It runs on the treasury jump host under a `screen` session
called `expo`. When the host reboots someone notices around 9am.

## What it does not have

- Tests. None. Not "few", none. No pytest in requirements, no `tests/` directory.
- A Dockerfile, Helm chart, Jenkinsfile, sonar or checkmarx configuration.
- Correlated VaR (TRS-0533).
- Limits in a table rather than in `main.py` (TRS-0611).

PLAT-2210 covers bringing it onto the platform. The first thing to do under that ticket is add a
test framework and a CI job, before touching the maths, because nobody can currently tell whether a
change to `engine.py` is right. Second is the Dockerfile. Do those before anything else and do not
let the desk add another endpoint first.

`scipy_stub.py` exists because scipy wheels were not on the approved list when this was written
(GIS-0977). It is the Acklam inverse normal. If scipy is approved now, replace it.
