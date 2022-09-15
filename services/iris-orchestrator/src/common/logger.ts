import { LoggerService } from '@nestjs/common';
import { correlation } from './correlation';
import { config } from '../config';

type Severity = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

/**
 * Splunk style structured logging, same field names as the Java common-starter so that a search
 * on correlationId returns bff-retail and bedrock-adapter events side by side (smoke.sh step 6).
 * Events go to stdout; the OpenShift log forwarder ships them to HEC. The direct HEC post below is
 * only used locally where there is no forwarder. Failures to reach HEC are swallowed on purpose.
 */
export class StructuredLogger implements LoggerService {
  constructor(private readonly context = 'app') {}

  log(message: unknown, context?: string): void {
    this.emit('INFO', message, context);
  }
  error(message: unknown, trace?: string, context?: string): void {
    this.emit('ERROR', message, context, trace);
  }
  warn(message: unknown, context?: string): void {
    this.emit('WARN', message, context);
  }
  debug(message: unknown, context?: string): void {
    this.emit('DEBUG', message, context);
  }
  verbose(message: unknown, context?: string): void {
    this.emit('DEBUG', message, context);
  }

  private emit(severity: Severity, message: unknown, context?: string, trace?: string): void {
    const record = {
      time: Date.now() / 1000,
      sourcetype: 'meridian:json',
      service: config.serviceName,
      environment: process.env.MERIDIAN_ENV ?? 'local',
      event: {
        event: typeof message === 'string' ? message : JSON.stringify(message),
        severity,
        logger: context ?? this.context,
        correlationId: correlation.current(),
        customerId: correlation.customerId(),
        trace,
      },
    };
    const line = JSON.stringify(record);
    if (severity === 'ERROR') {
      process.stderr.write(line + '\n');
    } else {
      process.stdout.write(line + '\n');
    }
    if (process.env.MERIDIAN_HEC_DIRECT === 'true') {
      // fire and forget; a logging outage must never take the BFF down (INC0048817)
      fetch(config.splunkHecUrl, {
        method: 'POST',
        headers: { Authorization: `Splunk ${config.splunkHecToken}`, 'Content-Type': 'application/json' },
        body: line,
      }).catch(() => undefined);
    }
  }
}
