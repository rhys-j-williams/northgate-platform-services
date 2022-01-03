from collections import defaultdict
from datetime import date
from typing import Dict, List

from dateutil.relativedelta import relativedelta

from .models import Period, Transaction


def _month_bounds(period: str):
    y, m = (int(x) for x in period.split("-"))
    start = date(y, m, 1)
    return start, start + relativedelta(months=1, days=-1)


def periods(txns: List[Transaction]) -> List[Period]:
    """Statement cycles are calendar months. The bank actually cycles on the account open day
    (Bedrock STMT-CYCLE-DD in MTBACCT) but the adapter does not surface it yet - PLAT-1406."""
    by_month: Dict[str, List[Transaction]] = defaultdict(list)
    for t in txns:
        if t.status == "pending":
            continue
        by_month[t.postedDate.strftime("%Y-%m")].append(t)
    out = []
    for key in sorted(by_month, reverse=True):
        start, end = _month_bounds(key)
        rows = sorted(by_month[key], key=lambda t: (t.postedDate, t.transactionId))
        out.append(Period(period=key, start=start, end=end, transactionCount=len(rows), closingBalanceMinor=rows[-1].runningBalanceMinor))
    return out


def in_period(txns: List[Transaction], period: str) -> List[Transaction]:
    start, end = _month_bounds(period)
    return sorted((t for t in txns if start <= t.postedDate <= end and t.status != "pending"), key=lambda t: (t.postedDate, t.transactionId))
