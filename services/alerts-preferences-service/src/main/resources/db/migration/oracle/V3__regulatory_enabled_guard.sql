-- GIS-0917 finding: nothing at the database layer stopped a regulatory row being disabled by a
-- direct UPDATE from a support tool. The service enforces it; this is belt and braces.
ALTER TABLE ALERT_PREFERENCE ADD CONSTRAINT CK_ALERT_PREF_REG_ENABLED CHECK (REGULATORY = 0 OR ENABLED = 1);
