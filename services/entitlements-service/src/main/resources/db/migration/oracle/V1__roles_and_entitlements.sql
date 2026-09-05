-- ENTL schema, Oracle 19c. Run through Flyway; the DBA team also has a copy for the Exadata
-- change record (CHG0048811). Keep the two in step.

CREATE TABLE ROLE_DEF (
    ROLE_CODE        VARCHAR2(32)   NOT NULL,
    DESCRIPTION      VARCHAR2(200)  NOT NULL,
    PERMISSIONS      VARCHAR2(1000) NOT NULL,   -- comma separated, see RoleCatalogue. Yes, really.
    SENSITIVE        NUMBER(1)      DEFAULT 0 NOT NULL,
    CONSTRAINT PK_ROLE_DEF PRIMARY KEY (ROLE_CODE)
);

CREATE TABLE ENTITLEMENT (
    ENTITLEMENT_ID          VARCHAR2(20)  NOT NULL,
    ORGANISATION_ID         VARCHAR2(20)  NOT NULL,
    CUSTOMER_ID             VARCHAR2(20)  NOT NULL,
    USER_HANDLE             VARCHAR2(64)  NOT NULL,
    ROLE_CODE               VARCHAR2(32)  NOT NULL,
    DUAL_APPROVAL_REQUIRED  NUMBER(1)     DEFAULT 0 NOT NULL,
    LIMIT_PER_TXN_MINOR     NUMBER(19),
    LIMIT_PER_DAY_MINOR     NUMBER(19),
    STATUS                  VARCHAR2(16)  DEFAULT 'ACTIVE' NOT NULL,
    CREATED_AT              TIMESTAMP     NOT NULL,
    UPDATED_AT              TIMESTAMP     NOT NULL,
    CONSTRAINT PK_ENTITLEMENT PRIMARY KEY (ENTITLEMENT_ID),
    CONSTRAINT FK_ENT_ROLE FOREIGN KEY (ROLE_CODE) REFERENCES ROLE_DEF (ROLE_CODE),
    CONSTRAINT CK_ENT_STATUS CHECK (STATUS IN ('ACTIVE', 'REVOKED', 'PENDING'))
);

CREATE INDEX IX_ENT_ORG ON ENTITLEMENT (ORGANISATION_ID, STATUS);
CREATE INDEX IX_ENT_USER ON ENTITLEMENT (USER_HANDLE, STATUS);

INSERT INTO ROLE_DEF VALUES ('administrator', 'Organisation administrator', 'users:manage,accounts:view,payments:initiate,payments:approve,reports:run,entitlements:manage', 1);
INSERT INTO ROLE_DEF VALUES ('approver',      'Payment approver',           'accounts:view,payments:approve,reports:run', 1);
INSERT INTO ROLE_DEF VALUES ('initiator',     'Payment initiator',          'accounts:view,payments:initiate,reports:run', 0);
INSERT INTO ROLE_DEF VALUES ('auditor',       'Read only plus audit view',  'accounts:view,reports:run,audit:view', 0);
INSERT INTO ROLE_DEF VALUES ('viewer',        'Read only',                  'accounts:view', 0);
