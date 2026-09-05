import os
from dataclasses import dataclass, field
from pathlib import Path


def _env(name: str, default: str) -> str:
    v = os.environ.get(name)
    return default if v is None or v == "" else v


@dataclass(frozen=True)
class Settings:
    service_name: str = "statements-api"
    port: int = int(_env("PORT", "4519"))
    bedrock_adapter_url: str = _env("BEDROCK_ADAPTER_URL", "http://localhost:4516/bedrock/v1")
    upstream_timeout_s: float = float(_env("UPSTREAM_TIMEOUT_S", "2.5"))
    # Fixture bundle exported from @meridian/domain-fixtures. Same file the Java services read.
    fixtures_path: Path = Path(
        _env("MERIDIAN_FIXTURES", str(Path(__file__).resolve().parents[3] / "fixtures" / "meridian-fixtures.json"))
    )
    fixture_fallback: bool = _env("MERIDIAN_FIXTURE_FALLBACK", "true") == "true"
    # Bank legal name and address for the statement header. Marketing owns the wording (MKT-0230).
    bank_name: str = "Meridian Trust Bank, N.A."
    bank_address: str = "PO Box 4400, Wilmington DE 19899"
    member_line: str = "Member FDIC. Equal Housing Lender."
    # PDF metadata. statements-api is what the archive team's ingest keys on (ARC-118), so do not
    # rename the producer string without telling them.
    producer: str = "meridian statements-api"
    splunk_hec_url: str = _env("SPLUNK_HEC_URL", "http://localhost:4606/services/collector/event")
    splunk_hec_token: str = _env("SPLUNK_HEC_TOKEN", "CHANGEME-splunk-hec-token")
    disclosures: tuple = field(
        default_factory=lambda: (
            "In case of errors or questions about your electronic transfers, contact us at the number on the back of your card "
            "or write to us at the address above as soon as you can. We must hear from you no later than 60 days after we sent "
            "the FIRST statement on which the problem or error appeared.",
            "Interest charge calculation: we figure the interest charge on your account by applying the periodic rate to the "
            "average daily balance of your account.",
        )
    )


settings = Settings()
