import { mkdtempSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';

process.env.NODE_ENV = 'test';
process.env.MERIDIAN_AUTH_MODE = 'insecure-local';
process.env.UPSTREAM_TIMEOUT_MS = '200';
process.env.STATEMENTS_API_URL = 'http://127.0.0.1:1';
process.env.OBJECT_STORE_ROOT = mkdtempSync(join(tmpdir(), 'docsvc-'));
