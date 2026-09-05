import { correlation } from './correlation';
import { config } from '../config';

/** Splunk shaped stdout logging; same fields as bff-retail and the Java common-starter. */
export function log(severity: 'INFO' | 'WARN' | 'ERROR', event: string, extra: Record<string, unknown> = {}): void {
  const line = JSON.stringify({
    time: Date.now() / 1000,
    sourcetype: 'meridian:json',
    service: config.serviceName,
    event: { event, severity, correlationId: correlation.current(), customerId: correlation.customerId(), ...extra },
  });
  (severity === 'ERROR' ? process.stderr : process.stdout).write(line + '\n');
}
