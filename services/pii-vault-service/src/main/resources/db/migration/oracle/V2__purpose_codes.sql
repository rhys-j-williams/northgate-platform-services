-- GIS-0417: detokenise calls must carry an approved purpose code. Codes are seeded here rather than
-- in the service so Compliance can add one with a DBA ticket instead of a release.
CREATE TABLE PURPOSE_CODE (
    PURPOSE              VARCHAR2(40)    NOT NULL,
    DESCRIPTION          VARCHAR2(120)   NOT NULL,
    ALLOWS_DETOKENISE    NUMBER(1)       DEFAULT 0 NOT NULL,
    CONSTRAINT PK_PURPOSE_CODE PRIMARY KEY (PURPOSE)
);

INSERT INTO PURPOSE_CODE VALUES ('ONBOARDING', 'Customer onboarding / KYC capture', 1);
INSERT INTO PURPOSE_CODE VALUES ('STATEMENT', 'Statement and tax document rendering', 1);
INSERT INTO PURPOSE_CODE VALUES ('DISPUTE', 'Reg E / Reg Z dispute handling', 1);
INSERT INTO PURPOSE_CODE VALUES ('SERVICING', 'Contact centre servicing, agent authenticated', 1);
INSERT INTO PURPOSE_CODE VALUES ('ANALYTICS', 'Analytics and reporting. Tokenise only.', 0);
INSERT INTO PURPOSE_CODE VALUES ('DISPLAY_MASKED', 'UI display of last four. Tokenise only.', 0);
