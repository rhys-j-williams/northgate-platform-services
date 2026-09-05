/** See bff-retail/src/config.ts for the pattern; this one is smaller. ConfigMap in OpenShift, PORTS.md locally. */
export type AuthMode = 'jwks' | 'insecure-local';

function env(name: string, fallback: string): string {
  const v = process.env[name];
  return v === undefined || v === '' ? fallback : v;
}

export const config = {
  port: Number(env('PORT', '4517')),
  serviceName: 'iris-orchestrator',
  authMode: env('MERIDIAN_AUTH_MODE', 'jwks') as AuthMode,
  keystoneIssuer: env('KEYSTONE_ISSUER', 'http://localhost:4400'),
  keystoneJwksUrl: env('KEYSTONE_JWKS_URL', 'http://localhost:4400/.well-known/jwks.json'),
  // Same audience as bff-retail: the widget reuses retail-web's token and we forward it on.
  keystoneAudience: env('KEYSTONE_AUDIENCE', 'api://meridian-digital-channels'),
  redisUrl: env('REDIS_URL', 'redis://localhost:6379/2'),
  cacheTtlSeconds: Number(env('SESSION_TTL_SECONDS', '1800')),
  bffRetailUrl: env('BFF_RETAIL_URL', 'http://localhost:4500/api/v1'),
  upstreamTimeoutMs: Number(env('UPSTREAM_TIMEOUT_MS', '2500')),
  intentsFile: env('IRIS_INTENTS_FILE', ''),
  /** Below this the engine answers with the fallback intent. Tuned by hand in PLAT-1433, do not touch without the transcripts. */
  confidenceThreshold: Number(env('IRIS_CONFIDENCE_THRESHOLD', '0.45')),
  /** Redis list the agent desktop (Semaphore) pops from. */
  handoffQueueKey: env('IRIS_HANDOFF_QUEUE', 'iris:handoff:v1'),
  fixtureFallback: env('MERIDIAN_FIXTURE_FALLBACK', 'true') === 'true',
  fixtureSeed: env('MERIDIAN_FIXTURE_SEED', 'meridian'),
  corsOrigins: env('CORS_ORIGINS', 'http://localhost:4200,http://localhost:4205').split(','),
  splunkHecUrl: env('SPLUNK_HEC_URL', 'http://localhost:4606/services/collector/event'),
  splunkHecToken: env('SPLUNK_HEC_TOKEN', 'CHANGEME-splunk-hec-token'),
};
