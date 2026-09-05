import logging
import uuid
from typing import List

from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.responses import JSONResponse

from . import bedrock, pdf
from .config import settings
from .logging_setup import configure, correlation_id
from .models import Period
from .periods import _month_bounds, in_period, periods

configure()
log = logging.getLogger("statements-api")

app = FastAPI(title="statements-api", version="3.4.2", docs_url=None, redoc_url=None, openapi_url=None)


@app.middleware("http")
async def correlation(request: Request, call_next):
    cid = (request.headers.get("x-correlation-id") or uuid.uuid4().hex)[:64]
    token = correlation_id.set(cid)
    try:
        response: Response = await call_next(request)
    finally:
        correlation_id.reset(token)
    response.headers["X-Correlation-Id"] = cid
    return response


def _error(status: int, code: str, message: str) -> JSONResponse:
    # Same envelope as common-starter ApiError; documents-service passes it through to the browser.
    from datetime import datetime, timezone

    return JSONResponse(
        status_code=status,
        content={"code": code, "message": message, "status": status, "correlationId": correlation_id.get(), "timestamp": datetime.now(timezone.utc).isoformat(), "violations": []},
    )


@app.exception_handler(HTTPException)
async def http_error(_: Request, exc: HTTPException):
    return _error(exc.status_code, exc.detail if isinstance(exc.detail, str) else "HTTP_ERROR", str(exc.detail))


@app.exception_handler(bedrock.BedrockUnavailable)
async def bedrock_down(_: Request, exc: bedrock.BedrockUnavailable):
    return _error(502, "BEDROCK_UNAVAILABLE", f"bedrock-adapter not reachable: {exc}")


@app.exception_handler(KeyError)
async def not_found(_: Request, exc: KeyError):
    return _error(404, "ACCOUNT_NOT_FOUND", f"no account {exc.args[0] if exc.args else ''}")


@app.get("/health")
def health():
    return {"status": "UP", "service": settings.service_name, "fixtureFallback": settings.fixture_fallback}


@app.get("/statements/v1/accounts/{account_id}/periods", response_model=List[Period])
def list_periods(account_id: str):
    bedrock.account(account_id)  # 404 before we do any work
    return periods(bedrock.transactions(account_id))


@app.get("/statements/v1/accounts/{account_id}/{period}.pdf")
def statement_pdf(account_id: str, period: str):
    if len(period) != 7 or period[4] != "-" or not (period[:4] + period[5:]).isdigit():
        raise HTTPException(status_code=400, detail="PERIOD_FORMAT")
    account = bedrock.account(account_id)
    txns = in_period(bedrock.transactions(account_id), period)
    start, end = _month_bounds(period)
    body = pdf.render_statement(account, period, start, end, txns)
    log.info("rendered statement account=%s period=%s bytes=%d txns=%d", account_id, period, len(body), len(txns))
    return Response(
        content=body,
        media_type="application/pdf",
        headers={"Content-Disposition": f'inline; filename="statement-{period}-{account.accountNumber[-4:]}.pdf"', "Cache-Control": "private, max-age=3600"},
    )


# No tests. There was a tests/ directory once; it was deleted in PLAT-0988 when the reportlab
# golden files drifted for the second time and nobody re-baselined them. The Jenkinsfile has no
# test stage. Sonar reports 0% and the quality gate is waived (SONAR-EX-0211).
