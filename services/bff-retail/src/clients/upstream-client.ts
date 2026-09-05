import { AxiosError, AxiosInstance } from 'axios';
import { ApiException } from '../common/api-error';
import { StructuredLogger } from '../common/logger';
import { upstream } from '../common/http';
import { config } from '../config';

/**
 * Base for the outbound clients. `callOrFallback` is the pattern every aggregation uses: try the
 * upstream, and if it is unreachable (not if it answered 4xx) serve the fixture answer when
 * MERIDIAN_FIXTURE_FALLBACK allows it. 4xx from an upstream are mapped to our error model.
 */
export abstract class UpstreamClient {
  protected readonly http: AxiosInstance;
  protected readonly logger: StructuredLogger;
  private fallbackAnnounced = false;

  protected constructor(protected readonly name: string, baseUrl: string) {
    this.http = upstream(baseUrl);
    this.logger = new StructuredLogger(name);
  }

  protected async callOrFallback<T>(call: () => Promise<T>, fallback: () => T | Promise<T>): Promise<T> {
    try {
      return await call();
    } catch (err) {
      const axiosErr = err as AxiosError<{ code?: string; message?: string }>;
      if (axiosErr.response) {
        const status = axiosErr.response.status;
        const body = axiosErr.response.data ?? {};
        if (status === 404) {
          throw ApiException.notFound(body.code ?? `${this.name.toUpperCase()}_NOT_FOUND`, body.message ?? 'not found');
        }
        if (status === 409) {
          throw ApiException.conflict(body.code ?? `${this.name.toUpperCase()}_CONFLICT`, body.message ?? 'conflict');
        }
        if (status === 403) {
          throw ApiException.forbidden(body.code ?? `${this.name.toUpperCase()}_FORBIDDEN`, body.message ?? 'forbidden');
        }
        if (status >= 400 && status < 500) {
          throw ApiException.badRequest(body.code ?? `${this.name.toUpperCase()}_REJECTED`, body.message ?? 'rejected upstream');
        }
      }
      if (!config.fixtureFallback) {
        throw ApiException.upstream(`${this.name.toUpperCase()}_UNAVAILABLE`, `${this.name} unavailable`);
      }
      if (!this.fallbackAnnounced) {
        this.logger.warn(`${this.name} unreachable (${axiosErr.message}); serving fixture data`);
        this.fallbackAnnounced = true;
      }
      return fallback();
    }
  }
}
