/** See bff-retail/src/config.ts; the two BFFs were split from one service in PLAT-1210 and kept the same shape. */
export type AuthMode = 'jwks' | 'insecure-local';

function env(name: string, fallback: string): string {
  const v = process.env[name];
  return v === undefined || v === '' ? fallback : v;
}

export const config = {
  port: Number(env('PORT', '4501')),
  serviceName: 'bff-business',
  authMode: env('MERIDIAN_AUTH_MODE', 'jwks') as AuthMode,
  keystoneIssuer: env('KEYSTONE_ISSUER', 'http://localhost:4400'),
  keystoneJwksUrl: env('KEYSTONE_JWKS_URL', 'http://localhost:4400/.well-known/jwks.json'),
  keystoneAudience: env('KEYSTONE_AUDIENCE', 'meridian-business'),
  redisUrl: env('REDIS_URL', 'redis://localhost:6379/1'),
  cacheTtlSeconds: Number(env('CACHE_TTL_SECONDS', '30')),
  bedrockAdapterUrl: env('BEDROCK_ADAPTER_URL', 'http://127.0.0.1:4516/bedrock/v1'),
  entitlementsUrl: env('ENTITLEMENTS_URL', 'http://127.0.0.1:4515/entitlements/v1'),
  txnPostingUrl: env('TXN_POSTING_URL', 'http://127.0.0.1:4512/posting/v1'),
  exposureCalcUrl: env('EXPOSURE_CALC_URL', 'http://127.0.0.1:4520'),
  upstreamTimeoutMs: Number(env('UPSTREAM_TIMEOUT_MS', '2500')),
  fixtureFallback: env('MERIDIAN_FIXTURE_FALLBACK', 'true') === 'true',
  fixtureSeed: env('MERIDIAN_FIXTURE_SEED', 'meridian'),
  corsOrigins: env('CORS_ORIGINS', 'http://localhost:4201,http://localhost:4203').split(','),
  splunkHecUrl: env('SPLUNK_HEC_URL', 'http://localhost:4606/services/collector/event'),
  splunkHecToken: env('SPLUNK_HEC_TOKEN', 'CHANGEME-splunk-hec-token'),
};
