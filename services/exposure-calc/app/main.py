"""exposure-calc. Treasury's position and exposure calculator.

Written by two people on the Treasury Technology team in a fortnight in 2022 (TRS-0410) so the
desk could stop running the numbers in a spreadsheet macro. It has never had a test, a Dockerfile,
a Helm chart, or a Jenkinsfile; it is started by hand on the treasury jump host with run.sh and
has been for two years. PLAT-2210 is the ticket to bring it into the platform properly and has
been re-parented three times. Do not add features here without also adding the infrastructure.

Everything is in memory and lost on restart. Positions are loaded from a fixture bundle or posted
in. Reference prices come from the TickerHaus mock when it is up and from a flat table when it is
not.
"""
import logging
import os
import uuid
from datetime import date, datetime, timezone
from typing import Dict, List, Optional

import numpy as np
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from .engine import Position, exposure_report, value_at_risk

logging.basicConfig(level=logging.INFO, format='{"time":"%(asctime)s","severity":"%(levelname)s","service":"exposure-calc","event":"%(message)s"}')
log = logging.getLogger("exposure-calc")

app = FastAPI(title="exposure-calc", version="0.9.3", docs_url=None, redoc_url=None, openapi_url=None)

BOOKS: Dict[str, List[Position]] = {}
LIMITS: Dict[str, int] = {
    # USD notional, from the 2024 treasury limits memo. Hard coded; TRS-0611 wants them in a table.
    "USD-RATES": 250_000_000,
    "FX-G10": 120_000_000,
    "LIQUIDITY": 500_000_000,
}


class PositionIn(BaseModel):
    instrumentId: str = Field(min_length=1, max_length=32)
    assetClass: str = Field(pattern="^(RATES|FX|CREDIT|CASH)$")
    currency: str = Field(min_length=3, max_length=3)
    notional: float
    price: float = Field(gt=0)
    duration: float = 0.0
    dailyVol: float = Field(default=0.005, ge=0)


class ScenarioIn(BaseModel):
    ratesShockBp: float = 0
    fxShockPct: float = 0
    creditSpreadBp: float = 0


@app.middleware("http")
async def correlation(request: Request, call_next):
    cid = request.headers.get("x-correlation-id") or uuid.uuid4().hex
    response = await call_next(request)
    response.headers["X-Correlation-Id"] = cid
    return response


@app.exception_handler(HTTPException)
async def http_error(_: Request, exc: HTTPException):
    return JSONResponse(status_code=exc.status_code, content={"code": str(exc.detail), "message": str(exc.detail), "status": exc.status_code, "timestamp": datetime.now(timezone.utc).isoformat(), "violations": []})


@app.get("/health")
def health():
    return {"status": "UP", "service": "exposure-calc", "books": len(BOOKS)}


@app.get("/exposure/v1/books")
def books():
    return {b: {"positions": len(p), "limit": LIMITS.get(b)} for b, p in BOOKS.items()}


@app.put("/exposure/v1/books/{book}/positions")
def load_positions(book: str, positions: List[PositionIn]):
    if book not in LIMITS:
        raise HTTPException(status_code=404, detail="BOOK_UNKNOWN")
    BOOKS[book] = [Position(**p.model_dump()) for p in positions]
    log.info("loaded %d positions into %s", len(positions), book)
    return {"book": book, "positions": len(BOOKS[book])}


@app.get("/exposure/v1/books/{book}/exposure")
def exposure(book: str, asOf: Optional[date] = None):
    positions = _book(book)
    report = exposure_report(positions)
    report["book"] = book
    report["asOf"] = (asOf or date.today()).isoformat()
    report["limit"] = LIMITS[book]
    report["limitUtilisationPct"] = round(100.0 * report["grossNotional"] / LIMITS[book], 2)
    report["breach"] = report["grossNotional"] > LIMITS[book]
    return report


@app.get("/exposure/v1/books/{book}/var")
def var(book: str, confidence: float = 0.99, horizonDays: int = 1):
    if not 0.5 < confidence < 1.0:
        raise HTTPException(status_code=400, detail="CONFIDENCE_RANGE")
    positions = _book(book)
    v = value_at_risk(positions, confidence=confidence, horizon_days=horizonDays)
    return {"book": book, "confidence": confidence, "horizonDays": horizonDays, "var": round(float(v), 2), "method": "parametric-normal-no-correlation"}


@app.post("/exposure/v1/books/{book}/scenario")
def scenario(book: str, s: ScenarioIn):
    positions = _book(book)
    pnl = 0.0
    for p in positions:
        if p.assetClass == "RATES":
            # DV01 approximation: notional * duration * bp shock / 10000, sign flips for long
            pnl -= p.notional * p.duration * s.ratesShockBp / 10_000
        elif p.assetClass == "FX":
            pnl += p.notional * s.fxShockPct / 100
        elif p.assetClass == "CREDIT":
            pnl -= p.notional * p.duration * s.creditSpreadBp / 10_000
    return {"book": book, "scenario": s.model_dump(), "pnl": round(pnl, 2)}


def _book(book: str) -> List[Position]:
    if book not in LIMITS:
        raise HTTPException(status_code=404, detail="BOOK_UNKNOWN")
    if book not in BOOKS:
        raise HTTPException(status_code=409, detail="BOOK_EMPTY")
    return BOOKS[book]


@app.on_event("startup")
def seed():
    """Seed a demo book so the desk sees numbers straight away. Not fixture data in the
    @meridian/domain-fixtures sense; treasury positions are not in that package (TRS-0592)."""
    if os.environ.get("EXPOSURE_SEED", "true") != "true":
        return
    rng = np.random.default_rng(20220314)
    BOOKS["USD-RATES"] = [
        Position(instrumentId=f"UST-{t}", assetClass="RATES", currency="USD", notional=float(rng.integers(5, 40) * 1_000_000) * (1 if rng.random() > 0.3 else -1), price=float(rng.uniform(95, 104)), duration=d, dailyVol=0.004 + 0.001 * d)
        for t, d in (("2Y", 1.9), ("5Y", 4.6), ("7Y", 6.3), ("10Y", 8.7), ("30Y", 17.2))
    ]
    BOOKS["FX-G10"] = [
        Position(instrumentId=f"{ccy}USD", assetClass="FX", currency=ccy, notional=float(rng.integers(2, 25) * 1_000_000) * (1 if rng.random() > 0.5 else -1), price=px, duration=0, dailyVol=v)
        for ccy, px, v in (("EUR", 1.08, 0.006), ("GBP", 1.27, 0.007), ("JPY", 0.0066, 0.008), ("CHF", 1.13, 0.006), ("CAD", 0.73, 0.005))
    ]
    log.info("seeded demo books")
