-- PLAT-1541 retries. ATTEMPTS is incremented by the dispatcher; the alert on ATTEMPTS >= 5 is in
-- the Splunk saved search "Beacon stuck dispatch", not here.
ALTER TABLE BEACON_NOTIFICATION ADD ATTEMPTS NUMBER(3) DEFAULT 0 NOT NULL;
ALTER TABLE BEACON_NOTIFICATION ADD NEXT_ATTEMPT_AT TIMESTAMP;
