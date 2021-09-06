"""Transaction and account source. bedrock-adapter first, fixture bundle second.

The adapter returns the copybook shaped records (MTBACCT / MTBTRAN, see platform-services/copybooks)
already unpacked to JSON. The fixture bundle is the raw @meridian/domain-fixtures export and has a
slightly different shape, hence the two mapping functions. They should be one function. PLAT-1722.
"""
import json
import logging
from datetime import date, datetime
from functools import lru_cache
from typing import List, Optional

import httpx

from .config import settings
from .logging_setup import correlation_id
from .models import Account, Transaction

log = logging.getLogger("bedrock")


class BedrockUnavailable(Exception):
    pass


def _headers() -> dict:
    return {"X-Correlation-Id": correlation_id.get(), "X-Channel": "STM"}


def _fix_date(s: Optional[str]) -> Optional[date]:
    if not s:
        return None
    return datetime.fromisoformat(s.replace("Z", "+00:00")).date()


@lru_cache(maxsize=1)
def _fixtures() -> dict:
    with settings.fixtures_path.open() as fh:
        return json.load(fh)


def account(account_id: str) -> Account:
    try:
        with httpx.Client(base_url=settings.bedrock_adapter_url, timeout=settings.upstream_timeout_s) as c:
            r = c.get(f"/accounts/{account_id}", headers=_headers())
            if r.status_code == 404:
                raise KeyError(account_id)
            r.raise_for_status()
            return Account(**r.json())
    except (httpx.HTTPError, OSError) as e:
        if not settings.fixture_fallback:
            raise BedrockUnavailable(str(e)) from e
        log.warning("bedrock-adapter unavailable for account %s, using fixtures: %s", account_id, e)
    for a in _fixtures()["accounts"]:
        if a["accountId"] == account_id:
            owner = next((c["displayName"] for c in _fixtures()["customers"] if c["customerId"] == a["customerId"]), None)
            return Account(ownerName=owner, **{k: a[k] for k in Account.model_fields if k in a})
    raise KeyError(account_id)


def transactions(account_id: str, limit: int = 2000) -> List[Transaction]:
    try:
        with httpx.Client(base_url=settings.bedrock_adapter_url, timeout=settings.upstream_timeout_s) as c:
            r = c.get(f"/accounts/{account_id}/transactions", params={"limit": limit}, headers=_headers())
            r.raise_for_status()
            return [Transaction(**t) for t in r.json()]
    except (httpx.HTTPError, OSError) as e:
        if not settings.fixture_fallback:
            raise BedrockUnavailable(str(e)) from e
        log.warning("bedrock-adapter unavailable for transactions %s, using fixtures: %s", account_id, e)
    out = []
    for t in _fixtures()["transactions"]:
        if t["accountId"] != account_id:
            continue
        out.append(
            Transaction(
                transactionId=t["transactionId"],
                accountId=t["accountId"],
                postedDate=_fix_date(t["postedAt"]),
                settledDate=_fix_date(t.get("settledAt")),
                amountMinor=t["amountMinor"],
                runningBalanceMinor=t["runningBalanceMinor"],
                description=t["description"],
                channel=t["channel"],
                status=t["status"],
                mcc=t.get("merchantCategoryCode"),
            )
        )
    out.sort(key=lambda t: (t.postedDate, t.transactionId))
    return out[-limit:]
