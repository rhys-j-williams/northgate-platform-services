-- PLAT-1350: auditor role was missing audit:view in prod because V1 had been hand applied by the
-- DBAs from an older draft. Idempotent on purpose.
UPDATE ROLE_DEF SET PERMISSIONS = 'accounts:view,reports:run,audit:view' WHERE ROLE_CODE = 'auditor';
