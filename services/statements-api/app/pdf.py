"""Statement rendering. reportlab platypus, one Table per statement.

Layout history: this is the third layout. The first (2020) was a hand positioned canvas and broke
whenever a description was long. The second used a frame per section and ran out of room on
accounts with more than ~180 transactions a month (INC0046120, a small business customer). This
one lets platypus paginate and repeats the header row. Fonts are the built in Helvetica because the
brand font licence does not cover server side rendering (MKT-0230).
"""
from datetime import date
from io import BytesIO
from typing import List

from reportlab.lib import colors
from reportlab.lib.enums import TA_RIGHT
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

from .config import settings
from .models import Account, Transaction

_styles = getSampleStyleSheet()
_small = ParagraphStyle("small", parent=_styles["Normal"], fontSize=7.5, leading=9)
_right = ParagraphStyle("right", parent=_styles["Normal"], alignment=TA_RIGHT)
_h = ParagraphStyle("h", parent=_styles["Heading2"], spaceBefore=6, spaceAfter=4)


def money(minor: int) -> str:
    sign = "-" if minor < 0 else ""
    return f"{sign}${abs(minor) / 100:,.2f}"


def mask(account_number: str) -> str:
    return "****" + account_number[-4:]


def _footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 7)
    canvas.drawString(0.75 * inch, 0.5 * inch, f"{settings.bank_name}  |  {settings.member_line}")
    canvas.drawRightString(letter[0] - 0.75 * inch, 0.5 * inch, f"Page {doc.page}")
    canvas.restoreState()


def render_statement(account: Account, period: str, start: date, end: date, txns: List[Transaction]) -> bytes:
    buf = BytesIO()
    doc = SimpleDocTemplate(
        buf,
        pagesize=letter,
        leftMargin=0.75 * inch,
        rightMargin=0.75 * inch,
        topMargin=0.75 * inch,
        bottomMargin=0.9 * inch,
        title=f"Statement {period} {mask(account.accountNumber)}",
        author=settings.bank_name,
        creator=settings.producer,
    )
    story = []
    story.append(Paragraph(settings.bank_name, _styles["Title"]))
    story.append(Paragraph(settings.bank_address, _styles["Normal"]))
    story.append(Spacer(1, 10))
    story.append(Paragraph(f"Account statement, {start.strftime('%B %d, %Y')} to {end.strftime('%B %d, %Y')}", _h))

    opening = txns[0].runningBalanceMinor - txns[0].amountMinor if txns else account.currentBalanceMinor
    closing = txns[-1].runningBalanceMinor if txns else account.currentBalanceMinor
    credits = sum(t.amountMinor for t in txns if t.amountMinor > 0)
    debits = sum(t.amountMinor for t in txns if t.amountMinor < 0)

    summary = Table(
        [
            ["Account holder", account.ownerName or "", "Opening balance", money(opening)],
            ["Account", f"{account.type}  {mask(account.accountNumber)}", "Deposits and credits", money(credits)],
            ["Routing", account.routingNumber, "Withdrawals and debits", money(debits)],
            ["Status", account.status, "Closing balance", money(closing)],
        ],
        colWidths=[1.2 * inch, 2.3 * inch, 1.8 * inch, 1.5 * inch],
        hAlign="LEFT",
    )
    summary.setStyle(
        TableStyle(
            [
                ("FONT", (0, 0), (-1, -1), "Helvetica", 9),
                ("FONT", (2, 0), (2, -1), "Helvetica-Bold", 9),
                ("ALIGN", (3, 0), (3, -1), "RIGHT"),
                ("LINEBELOW", (0, -1), (-1, -1), 0.5, colors.grey),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
            ]
        )
    )
    story.append(summary)
    story.append(Spacer(1, 12))
    story.append(Paragraph("Transactions", _h))

    rows = [["Date", "Description", "Channel", "Amount", "Balance"]]
    for t in txns:
        rows.append([t.postedDate.strftime("%m/%d"), Paragraph(t.description[:60], _styles["Normal"]), t.channel.upper(), money(t.amountMinor), money(t.runningBalanceMinor)])
    if not txns:
        rows.append(["", "No transactions this period", "", "", ""])
    table = Table(rows, colWidths=[0.6 * inch, 3.4 * inch, 0.8 * inch, 1.0 * inch, 1.0 * inch], repeatRows=1, hAlign="LEFT")
    table.setStyle(
        TableStyle(
            [
                ("FONT", (0, 0), (-1, 0), "Helvetica-Bold", 8.5),
                ("FONT", (0, 1), (-1, -1), "Helvetica", 8.5),
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E8EEF4")),
                ("ALIGN", (3, 0), (-1, -1), "RIGHT"),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F7F9FB")]),
                ("LINEBELOW", (0, 0), (-1, 0), 0.5, colors.grey),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("TOPPADDING", (0, 0), (-1, -1), 2),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 2),
            ]
        )
    )
    story.append(table)
    story.append(Spacer(1, 14))
    story.append(Paragraph("Important information", _h))
    for d in settings.disclosures:
        story.append(Paragraph(d, _small))
        story.append(Spacer(1, 3))
    # TODO(PLAT-1406) statement cycle date from MTBACCT once the adapter exposes it
    doc.build(story, onFirstPage=_footer, onLaterPages=_footer)
    return buf.getvalue()
