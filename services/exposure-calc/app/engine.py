from dataclasses import dataclass
from typing import Dict, List

import numpy as np
from scipy_stub import norm_ppf


@dataclass
class Position:
    instrumentId: str
    assetClass: str
    currency: str
    notional: float
    price: float
    duration: float = 0.0
    dailyVol: float = 0.005


def exposure_report(positions: List[Position]) -> Dict:
    notionals = np.array([p.notional for p in positions], dtype=float)
    mv = notionals * np.array([p.price for p in positions]) / 100.0 if positions and positions[0].assetClass == "RATES" else notionals
    by_ccy: Dict[str, float] = {}
    for p, m in zip(positions, mv):
        by_ccy[p.currency] = by_ccy.get(p.currency, 0.0) + float(m)
    dv01 = float(np.sum(notionals * np.array([p.duration for p in positions]) / 10_000))
    return {
        "positions": len(positions),
        "grossNotional": float(np.abs(notionals).sum()),
        "netNotional": float(notionals.sum()),
        "marketValue": float(mv.sum()),
        "dv01": round(dv01, 2),
        "byCurrency": {k: round(v, 2) for k, v in by_ccy.items()},
        "largest": max(positions, key=lambda p: abs(p.notional)).instrumentId if positions else None,
    }


def value_at_risk(positions: List[Position], confidence: float = 0.99, horizon_days: int = 1) -> float:
    """Parametric VaR with no correlation matrix, i.e. it assumes every position moves
    independently, which understates the rates book badly. The desk knows (TRS-0533). A proper
    covariance from TickerHaus history is the next thing PLAT-2210 should do after the tests."""
    if not positions:
        return 0.0
    exposures = np.array([abs(p.notional) * p.dailyVol for p in positions])
    z = norm_ppf(confidence)
    return float(z * np.sqrt(np.sum(exposures**2)) * np.sqrt(horizon_days))
