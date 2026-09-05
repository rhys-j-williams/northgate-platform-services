-- PLAT-1188: postings accepted while Bedrock is in EOD are held as PENDING_BEDROCK and replayed by
-- the 05:30 sweep. Before this, the service returned 503 and the mobile app retried into a loop.
ALTER TABLE POSTING ADD CONSTRAINT CK_POSTING_STATUS
    CHECK (STATUS IN ('POSTED', 'DUPLICATE', 'REFUSED', 'PENDING_BEDROCK', 'REVERSED'));

CREATE INDEX IX_POSTING_PENDING ON POSTING (STATUS, CREATED_AT);
