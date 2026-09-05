-- pii-vault-service, Oracle dialect (H2 MODE=Oracle locally). PIIVAULT schema, encrypted tablespace
-- (TDE) in the bank. This is the one schema the DBAs will not give anybody SELECT on without a
-- GIS ticket; the runbook explains how to ask.
--
-- TOKEN_MAP: token -> ciphertext of the original. The FPE token alone is reversible with the key,
-- so strictly we do not need this table; it exists because Audit want to know *which* values have
-- ever been tokenised, and because rotating the FPE key without it would orphan every token.

CREATE SEQUENCE TOKEN_MAP_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE PII_ACCESS_SEQ START WITH 1 INCREMENT BY 100;

CREATE TABLE TOKEN_MAP (
    TOKEN_MAP_ID         NUMBER(19)      NOT NULL,
    TOKEN                VARCHAR2(64)    NOT NULL,
    PII_TYPE             VARCHAR2(16)    NOT NULL,
    KEY_VERSION          NUMBER(5)       NOT NULL,
    CIPHERTEXT           VARCHAR2(512)   NOT NULL,
    VALUE_HASH           VARCHAR2(64)    NOT NULL,
    CREATED_BY           VARCHAR2(64)    NOT NULL,
    CREATED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_TOKEN_MAP PRIMARY KEY (TOKEN_MAP_ID),
    CONSTRAINT UQ_TOKEN_MAP_TOKEN UNIQUE (TOKEN)
);

CREATE INDEX IX_TOKEN_MAP_HASH ON TOKEN_MAP (VALUE_HASH);

-- Every tokenise and every detokenise. Append only. GLBA / state privacy law evidence.
CREATE TABLE PII_ACCESS (
    ACCESS_ID            NUMBER(19)      NOT NULL,
    OPERATION            VARCHAR2(12)    NOT NULL,
    PII_TYPE             VARCHAR2(16)    NOT NULL,
    TOKEN                VARCHAR2(64),
    PRINCIPAL            VARCHAR2(64)    NOT NULL,
    CALLING_SERVICE      VARCHAR2(64),
    PURPOSE              VARCHAR2(40)    NOT NULL,
    OUTCOME              VARCHAR2(12)    NOT NULL,
    CORRELATION_ID       VARCHAR2(64),
    ACCESSED_AT          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_PII_ACCESS PRIMARY KEY (ACCESS_ID)
);

CREATE INDEX IX_PII_ACCESS_TOKEN ON PII_ACCESS (TOKEN, ACCESSED_AT);
CREATE INDEX IX_PII_ACCESS_PRINCIPAL ON PII_ACCESS (PRINCIPAL, ACCESSED_AT);
