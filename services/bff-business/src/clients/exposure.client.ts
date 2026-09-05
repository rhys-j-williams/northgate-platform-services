import { Injectable } from '@nestjs/common';
import { UpstreamClient } from './upstream-client';
import { FixturesService } from './fixtures.service';
import { config } from '../config';

export interface Position {
  accountId: string;
  currency: 'USD';
  balanceMinor: number;
  bucket: 'operating' | 'reserve' | 'debt';
}

export interface ExposureReport {
  customerId: string;
  asOf: string;
  netPositionMinor: number;
  grossExposureMinor: number;
  concentration: Array<{ bucket: string; sharePct: number }>;
  var95Minor?: number;
}

@Injectable()
export class ExposureClient extends UpstreamClient {
  constructor(private readonly fixtures: FixturesService) {
    super('exposure-calc', config.exposureCalcUrl);
  }

  exposure(customerId: string, positions: Position[]): Promise<ExposureReport> {
    return this.callOrFallback(
      async () => (await this.http.post<ExposureReport>('/v1/exposure', { customerId, positions })).data,
      () => {
        // arithmetic only, no VaR: that needs numpy on the other side
        const gross = positions.reduce((s, p) => s + Math.abs(p.balanceMinor), 0);
        const net = positions.reduce((s, p) => s + p.balanceMinor, 0);
        const byBucket = new Map<string, number>();
        for (const p of positions) {
          byBucket.set(p.bucket, (byBucket.get(p.bucket) ?? 0) + Math.abs(p.balanceMinor));
        }
        return {
          customerId,
          asOf: new Date().toISOString(),
          netPositionMinor: net,
          grossExposureMinor: gross,
          concentration: [...byBucket.entries()].map(([bucket, v]) => ({ bucket, sharePct: gross === 0 ? 0 : Math.round((v / gross) * 10000) / 100 })),
        };
      },
    );
  }
}
