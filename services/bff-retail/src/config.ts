/**
 * Runtime configuration. In OpenShift these come from the ConfigMap rendered by the Helm chart
 * (helm/values-*.yaml) plus the Vault agent sidecar for anything secret. Locally the defaults point
 * at the mock-external ports in PORTS.md.
 */
export type AuthMode = 'jwks' | 'insecure-local';

function env(name: string, fallback: string): string {
  const v = process.env[name];
  return v === undefined || v === '' ? fallback : v;
}

export const config = {
  port: Number(env('PORT', '4500')),
  serviceName: 'bff-retail',
  authMode: env('MERIDIAN_AUTH_MODE', 'jwks') as AuthMode,
  keystoneIssuer: env('KEYSTONE_ISSUER', 'http://localhost:4400'),
  keystoneJwksUrl: env('KEYSTONE_JWKS_URL', 'http://localhost:4400/.well-known/jwks.json'),
  keystoneAudience: env('KEYSTONE_AUDIENCE', 'meridian-retail'),
  redisUrl: env('REDIS_URL', 'redis://localhost:6379/0'),
  cacheTtlSeconds: Number(env('CACHE_TTL_SECONDS', '30')),
  bedrockAdapterUrl: env('BEDROCK_ADAPTER_URL', 'http://localhost:4516/bedrock/v1'),
  txnPostingUrl: env('TXN_POSTING_URL', 'http://localhost:4512/posting/v1'),
  alertsPreferencesUrl: env('ALERTS_PREFERENCES_URL', 'http://localhost:4511/preferences/v1'),
  aggregioUrl: env('AGGREGIO_URL', 'http://localhost:4601'),
  tickerhausUrl: env('TICKERHAUS_URL', 'http://localhost:4602'),
  triscoreUrl: env('TRISCORE_URL', 'http://localhost:4603'),
  paylinkUrl: env('PAYLINK_URL', 'http://localhost:4604'),
  upstreamTimeoutMs: Number(env('UPSTREAM_TIMEOUT_MS', '2500')),
  /** When an upstream is down, answer from @meridian/domain-fixtures instead of 502. Never on in prod. */
  fixtureFallback: env('MERIDIAN_FIXTURE_FALLBACK', 'true') === 'true',
  fixtureSeed: env('MERIDIAN_FIXTURE_SEED', 'meridian'),
  corsOrigins: env('CORS_ORIGINS', 'http://localhost:4200,http://localhost:4205').split(','),
  splunkHecUrl: env('SPLUNK_HEC_URL', 'http://localhost:4606/services/collector/event'),
  splunkHecToken: env('SPLUNK_HEC_TOKEN', 'CHANGEME-splunk-hec-token'),
};
