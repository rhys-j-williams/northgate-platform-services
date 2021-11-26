import axios, { AxiosInstance } from 'axios';
import { Readable } from 'stream';
import { config } from '../config';
import { correlation } from '../common/correlation';
import { ApiError } from '../common/api-error';

export interface StatementPeriod {
  period: string;
  start: string;
  end: string;
  transactionCount: number;
  closingBalanceMinor: number;
}

export class StatementsApiClient {
  private readonly http: AxiosInstance = axios.create({ baseURL: config.statementsApiUrl, timeout: config.upstreamTimeoutMs });

  constructor() {
    this.http.interceptors.request.use((req) => {
      req.headers['X-Correlation-Id'] = correlation.current();
      return req;
    });
  }

  async periods(accountId: string): Promise<StatementPeriod[]> {
    try {
      return (await this.http.get<StatementPeriod[]>(`/statements/v1/accounts/${encodeURIComponent(accountId)}/periods`)).data;
    } catch (err) {
      throw this.map(err);
    }
  }

  /** Streams the PDF; the caller tees it into the object store and the response. */
  async pdf(accountId: string, period: string): Promise<{ stream: Readable; length?: number }> {
    try {
      const res = await this.http.get<Readable>(`/statements/v1/accounts/${encodeURIComponent(accountId)}/${period}.pdf`, { responseType: 'stream' });
      const len = res.headers['content-length'];
      return { stream: res.data, length: len ? Number(len) : undefined };
    } catch (err) {
      throw this.map(err);
    }
  }

  private map(err: unknown): ApiError {
    if (axios.isAxiosError(err) && err.response) {
      const body = err.response.data as { code?: string; message?: string } | undefined;
      return new ApiError(err.response.status === 404 ? 404 : 502, body?.code ?? 'STATEMENTS_API_ERROR', body?.message ?? err.message);
    }
    return new ApiError(503, 'STATEMENTS_API_UNAVAILABLE', `statements-api not reachable: ${(err as Error).message}`);
  }
}
