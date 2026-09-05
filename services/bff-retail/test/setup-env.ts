process.env.NODE_ENV = 'test';
process.env.MERIDIAN_AUTH_MODE = 'insecure-local';
process.env.MERIDIAN_FIXTURE_FALLBACK = 'true';
process.env.UPSTREAM_TIMEOUT_MS = '200';
// nothing listens here; forces the fixture fallback path quickly
process.env.BEDROCK_ADAPTER_URL = 'http://127.0.0.1:1/bedrock/v1';
process.env.TICKERHAUS_URL = 'http://127.0.0.1:1';
process.env.TRISCORE_URL = 'http://127.0.0.1:1';
process.env.AGGREGIO_URL = 'http://127.0.0.1:1';
process.env.PAYLINK_URL = 'http://127.0.0.1:1';
process.env.TXN_POSTING_URL = 'http://127.0.0.1:1';
