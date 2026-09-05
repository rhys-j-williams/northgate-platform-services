-- Reversal window is 60 days (Reg E error-resolution period). Stored so the batch and the service
-- agree; the service also hard-codes it, which is PLAT-1301 and nobody has picked it up.
CREATE TABLE POSTING_PARAM (
    PARAM_KEY            VARCHAR2(40)    NOT NULL,
    PARAM_VALUE          VARCHAR2(80)    NOT NULL,
    UPDATED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_POSTING_PARAM PRIMARY KEY (PARAM_KEY)
);

INSERT INTO POSTING_PARAM (PARAM_KEY, PARAM_VALUE) VALUES ('REVERSAL_WINDOW_DAYS', '60');
INSERT INTO POSTING_PARAM (PARAM_KEY, PARAM_VALUE) VALUES ('DAILY_DEBIT_LIMIT_MINOR', '2500000');
