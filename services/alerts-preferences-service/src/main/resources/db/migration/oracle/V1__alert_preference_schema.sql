-- alerts-preferences-service schema, Oracle dialect. Locally this runs on H2 MODE=Oracle; in the
-- bank it is the ALERTPREF schema on the same Exadata as Beacon (PLAT-0412). Sequences, not
-- identity columns: the prod pipeline runs this against 19c and identity broke on the 2021 attempt.

CREATE SEQUENCE ALERT_PREF_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE ALERT_PREF_HIST_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE ALERT_PREFERENCE (
    PREFERENCE_ID        NUMBER(19)      NOT NULL,
    CUSTOMER_ID          VARCHAR2(16)    NOT NULL,
    ALERT_CODE           VARCHAR2(40)    NOT NULL,
    LABEL                VARCHAR2(80)    NOT NULL,
    REGULATORY           NUMBER(1)       DEFAULT 0 NOT NULL,
    ENABLED              NUMBER(1)       DEFAULT 1 NOT NULL,
    CHANNELS             VARCHAR2(120)   NOT NULL,
    THRESHOLD_MINOR      NUMBER(19),
    QUIET_HOURS_START    VARCHAR2(5),
    QUIET_HOURS_END      VARCHAR2(5),
    VERSION_NO           NUMBER(10)      DEFAULT 0 NOT NULL,
    UPDATED_BY           VARCHAR2(64),
    UPDATED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_ALERT_PREFERENCE PRIMARY KEY (PREFERENCE_ID),
    CONSTRAINT UQ_ALERT_PREF_CUST_CODE UNIQUE (CUSTOMER_ID, ALERT_CODE)
);

CREATE INDEX IX_ALERT_PREF_CUST ON ALERT_PREFERENCE (CUSTOMER_ID);

-- Change history. Reg E / Reg DD want to know what the customer had switched on at any given date.
-- Rows are written by the service; nothing deletes from here. Retention job is a DBA-side purge
-- after 7 years (CAB-2022-118).
CREATE TABLE ALERT_PREFERENCE_HIST (
    HIST_ID              NUMBER(19)      NOT NULL,
    PREFERENCE_ID        NUMBER(19)      NOT NULL,
    CUSTOMER_ID          VARCHAR2(16)    NOT NULL,
    ALERT_CODE           VARCHAR2(40)    NOT NULL,
    ENABLED_BEFORE       NUMBER(1),
    ENABLED_AFTER        NUMBER(1),
    CHANNELS_BEFORE      VARCHAR2(120),
    CHANNELS_AFTER       VARCHAR2(120),
    CHANGED_BY           VARCHAR2(64),
    CORRELATION_ID       VARCHAR2(64),
    CHANGED_AT           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_ALERT_PREFERENCE_HIST PRIMARY KEY (HIST_ID)
);

CREATE INDEX IX_ALERT_PREF_HIST_CUST ON ALERT_PREFERENCE_HIST (CUSTOMER_ID, CHANGED_AT);
