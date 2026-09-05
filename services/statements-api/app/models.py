from datetime import date
from typing import Optional

from pydantic import BaseModel


class Account(BaseModel):
    accountId: str
    customerId: str
    type: str
    accountNumber: str
    routingNumber: str
    currentBalanceMinor: int
    availableBalanceMinor: int
    status: str
    ownerName: Optional[str] = None
    nickname: Optional[str] = None


class Transaction(BaseModel):
    transactionId: str
    accountId: str
    postedDate: date
    settledDate: Optional[date] = None
    amountMinor: int
    runningBalanceMinor: int
    description: str
    channel: str
    status: str
    mcc: Optional[str] = None


class Period(BaseModel):
    period: str  # YYYY-MM
    start: date
    end: date
    transactionCount: int
    closingBalanceMinor: int
