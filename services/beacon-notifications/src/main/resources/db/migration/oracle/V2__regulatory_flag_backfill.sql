-- PLAT-1188: regulatory notices dispatched before 5.0 were written with REGULATORY = 0 because the
-- template registry did not carry the flag. Backfill by template code so the retention job
-- (7 years for regulatory, 18 months otherwise) keeps the right rows.
UPDATE BEACON_NOTIFICATION SET REGULATORY = 1
 WHERE TEMPLATE_CODE IN ('REG_OVERDRAFT_NOTICE', 'REG_RATE_CHANGE', 'REG_PRIVACY_ANNUAL', 'REG_ERROR_RESOLUTION');
