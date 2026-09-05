import json
import logging
import sys
import time
from contextvars import ContextVar

from .config import settings

correlation_id: ContextVar[str] = ContextVar("correlation_id", default="no-request")


class SplunkJsonFormatter(logging.Formatter):
    """Same field names as the Java common-starter MeridianLayout so Splunk joins the hops."""

    def format(self, record: logging.LogRecord) -> str:
        return json.dumps(
            {
                "time": time.time(),
                "sourcetype": "meridian:json",
                "service": settings.service_name,
                "event": {
                    "event": record.getMessage(),
                    "severity": record.levelname,
                    "logger": record.name,
                    "correlationId": correlation_id.get(),
                },
            }
        )


def configure() -> None:
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(SplunkJsonFormatter())
    root = logging.getLogger()
    root.handlers = [handler]
    root.setLevel(logging.INFO)
    # uvicorn's own access log is noise next to ours; keep errors only
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
