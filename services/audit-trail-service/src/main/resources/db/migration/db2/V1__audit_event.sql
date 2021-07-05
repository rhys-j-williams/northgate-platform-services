-- audit-trail-service, DB2 dialect. Locally H2 MODE=DB2; in the bank this is AUDITDB on the
-- DB2 11.5 LUW pair in DC2 (the only DB2 left after the 2019 consolidation, kept because Internal
-- Audit's extraction tooling is written against it and nobody wants to be the one to break it).
--
-- Note CURRENT_TIMESTAMP with the underscore: DB2 accepts both spellings, H2 only the one. PLAT-0966.
--
-- Append only. The service account has INSERT and SELECT. UPDATE/DELETE are revoked at the grant
-- level (see runbook, "grants"); in H2 we cannot express that, so the service relies on the hash
-- chain in PREV_HASH / EVENT_HASH for tamper evidence and never issues an UPDATE.

CREATE SEQUENCE AUDIT_EVENT_SEQ AS BIGINT START WITH 1 INCREMENT BY 100 NO CYCLE;

CREATE TABLE AUDIT_EVENT (
    EVENT_ID          BIGINT         NOT NULL,
    EVENT_TIME        TIMESTAMP      NOT NULL,
    RECEIVED_AT       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    SOURCE_SERVICE    VARCHAR(64)    NOT NULL,
    EVENT_TYPE        VARCHAR(64)    NOT NULL,
    SUBJECT_TYPE      VARCHAR(32)    NOT NULL,
    SUBJECT_ID        VARCHAR(64)    NOT NULL,
    ACTOR             VARCHAR(64)    NOT NULL,
    OUTCOME           VARCHAR(16)    NOT NULL,
    CORRELATION_ID    VARCHAR(64),
    SOURCE_TOPIC      VARCHAR(80),
    SOURCE_OFFSET     BIGINT,
    PAYLOAD           CLOB(64K),
    PREV_HASH         CHAR(64)       NOT NULL,
    EVENT_HASH        CHAR(64)       NOT NULL,
    CONSTRAINT PK_AUDIT_EVENT PRIMARY KEY (EVENT_ID)
);

CREATE INDEX IX_AUDIT_SUBJECT ON AUDIT_EVENT (SUBJECT_TYPE, SUBJECT_ID, EVENT_TIME);
CREATE INDEX IX_AUDIT_ACTOR ON AUDIT_EVENT (ACTOR, EVENT_TIME);
CREATE INDEX IX_AUDIT_CORR ON AUDIT_EVENT (CORRELATION_ID);
