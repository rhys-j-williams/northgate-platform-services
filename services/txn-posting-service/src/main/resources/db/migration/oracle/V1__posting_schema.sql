-- txn-posting-service schema, Oracle dialect (H2 MODE=Oracle locally). In the bank: TXNPOST schema,
-- Exadata, same DB service as Beacon but separate tablespace after INC-2022-0410 (Beacon's
-- notification purge locked our extents for eleven minutes).
--
-- POSTING is the ledger of every request we accepted. Bedrock is the book of record; this table is
-- what we show the customer while Bedrock is in EOD and what we reconcile against at 05:30.

CREATE SEQUENCE POSTING_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE POSTING (
    POSTING_ID           NUMBER(19)      NOT NULL,
    IDEMPOTENCY_KEY      VARCHAR2(36)    NOT NULL,
    REQUEST_HASH         VARCHAR2(64)    NOT NULL,
    ACCOUNT_ID           VARCHAR2(16)    NOT NULL,
    POSTING_TYPE         VARCHAR2(8)     NOT NULL,
    AMOUNT_MINOR         NUMBER(19)      NOT NULL,
    DESCRIPTION          VARCHAR2(80),
    CHANNEL              VARCHAR2(3)     NOT NULL,
    STATUS               VARCHAR2(16)    NOT NULL,
    BEDROCK_TRAN_ID      VARCHAR2(16),
    NEW_BALANCE_MINOR    NUMBER(19),
    REFUSAL_REASON       VARCHAR2(40),
    ORIGINAL_POSTING_ID  NUMBER(19),
    REVERSED_BY_ID       NUMBER(19),
    CORRELATION_ID       VARCHAR2(64),
    CREATED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_POSTING PRIMARY KEY (POSTING_ID),
    CONSTRAINT UQ_POSTING_IDEM UNIQUE (IDEMPOTENCY_KEY)
);

CREATE INDEX IX_POSTING_ACCT ON POSTING (ACCOUNT_ID, CREATED_AT);
CREATE INDEX IX_POSTING_BEDROCK ON POSTING (BEDROCK_TRAN_ID);
