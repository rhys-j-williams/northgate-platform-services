import { join } from 'path';

function env(name: string, fallback: string): string {
  const v = process.env[name];
  return v === undefined || v === '' ? fallback : v;
}

export const config = {
  port: Number(env('PORT', '4518')),
  serviceName: 'documents-service',
  authMode: env('MERIDIAN_AUTH_MODE', 'jwks') as 'jwks' | 'insecure-local',
  keystoneIssuer: env('KEYSTONE_ISSUER', 'http://localhost:4400'),
  keystoneJwksUrl: env('KEYSTONE_JWKS_URL', 'http://localhost:4400/.well-known/jwks.json'),
  // Accepts both audiences: retail-web and business-web both download statements from here.
  keystoneAudiences: env('KEYSTONE_AUDIENCES', 'api://meridian-digital-channels,meridian-retail,meridian-business').split(','),
  statementsApiUrl: env('STATEMENTS_API_URL', 'http://127.0.0.1:4519'),
  bedrockAdapterUrl: env('BEDROCK_ADAPTER_URL', 'http://localhost:4516/bedrock/v1'),
  upstreamTimeoutMs: Number(env('UPSTREAM_TIMEOUT_MS', '8000')),
  /**
   * Object store. In the bank this is the on-prem S3 compatible appliance (bucket meridian-docs-prod,
   * WORM enabled, 7 year retention for statements per RET-POL-04). Locally it is a directory.
   */
  objectStoreRoot: env('OBJECT_STORE_ROOT', join(process.cwd(), 'var', 'objectstore')),
  fixtureFallback: env('MERIDIAN_FIXTURE_FALLBACK', 'true') === 'true',
  fixtureSeed: env('MERIDIAN_FIXTURE_SEED', 'meridian'),
  corsOrigins: env('CORS_ORIGINS', 'http://localhost:4200,http://localhost:4201,http://localhost:4203').split(','),
  splunkHecUrl: env('SPLUNK_HEC_URL', 'http://localhost:4606/services/collector/event'),
  splunkHecToken: env('SPLUNK_HEC_TOKEN', 'CHANGEME-splunk-hec-token'),
};
