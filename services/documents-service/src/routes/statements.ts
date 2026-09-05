import { Router } from 'express';
import { PassThrough } from 'stream';
import { ObjectStore } from '../store/object-store';
import { StatementsApiClient } from '../clients/statements-api.client';
import { ApiError } from '../common/api-error';
import { log } from '../common/logger';
import { fixtures } from '../fixtures';
import { config } from '../config';

const PERIOD = /^\d{4}-(0[1-9]|1[0-2])$/;

/**
 * Statement list and download. The download is read-through: object store first, statements-api
 * on a miss, and the miss is written to the store while it streams to the browser so the next
 * download (the customer always clicks twice) is served locally.
 */
export function statementsRouter(store: ObjectStore, statements: StatementsApiClient): Router {
  const r = Router();

  r.get('/statements', async (req, res, next) => {
    try {
      const customerId = req.principal!.customerId;
      const accounts = ownedAccounts(customerId);
      const out = [];
      for (const accountId of accounts) {
        let periods;
        try {
          periods = await statements.periods(accountId);
        } catch (err) {
          if (!config.fixtureFallback) {
            throw err;
          }
          log('WARN', `statements-api unavailable, listing from archive only: ${(err as Error).message}`);
          periods = store.list(`statements/${accountId}/`).map((m) => ({ period: m.key.split('/').pop()!.replace('.pdf', ''), transactionCount: undefined, closingBalanceMinor: undefined }));
        }
        for (const p of periods) {
          const archived = store.head(key(accountId, p.period));
          out.push({ accountId, period: p.period, transactionCount: p.transactionCount, closingBalanceMinor: p.closingBalanceMinor, archived: archived !== undefined, sizeBytes: archived?.size, href: `/documents/v1/statements/${accountId}/${p.period}.pdf` });
        }
      }
      res.json(out);
    } catch (err) {
      next(err);
    }
  });

  r.get('/statements/:accountId/:period.pdf', async (req, res, next) => {
    try {
      const { accountId } = req.params;
      let { period } = req.params;
      if (period !== 'latest' && !PERIOD.test(period)) {
        throw new ApiError(400, 'PERIOD_FORMAT', 'period must be YYYY-MM or latest');
      }
      if (!ownedAccounts(req.principal!.customerId).includes(accountId)) {
        // 404 not 403; do not confirm the account exists (GIS-1204)
        throw new ApiError(404, 'STATEMENT_NOT_FOUND', 'no such statement');
      }
      if (period === 'latest') {
        period = await latestPeriod(accountId, store, statements);
      }
      const k = key(accountId, period);
      res.setHeader('Content-Type', 'application/pdf');
      res.setHeader('Content-Disposition', `inline; filename="statement-${period}-${accountId.slice(-4)}.pdf"`);
      res.setHeader('Cache-Control', 'private, no-store');

      const hit = store.head(k);
      if (hit) {
        res.setHeader('Content-Length', String(hit.size));
        res.setHeader('X-Meridian-Source', 'archive');
        store.get(k).pipe(res);
        return;
      }
      const { stream, length } = await statements.pdf(accountId, period);
      if (length) {
        res.setHeader('Content-Length', String(length));
      }
      res.setHeader('X-Meridian-Source', 'statements-api');
      const toStore = new PassThrough();
      const toClient = new PassThrough();
      stream.pipe(toStore);
      stream.pipe(toClient);
      toClient.pipe(res);
      store.put(k, toStore).then((m) => log('INFO', 'statement archived', { key: k, size: m.size })).catch((err: Error) => log('WARN', `archive write failed: ${err.message}`, { key: k }));
    } catch (err) {
      next(err);
    }
  });

  return r;
}

/** Newest period statements-api knows about; newest archived one if statements-api is down. */
async function latestPeriod(accountId: string, store: ObjectStore, statements: StatementsApiClient): Promise<string> {
  let periods: string[];
  try {
    periods = (await statements.periods(accountId)).map((p) => p.period);
  } catch (err) {
    if (!config.fixtureFallback) {
      throw err;
    }
    periods = store.list(`statements/${accountId}/`).map((m) => m.key.split('/').pop()!.replace('.pdf', ''));
  }
  periods = periods.filter((p) => PERIOD.test(p)).sort();
  if (periods.length === 0) {
    throw new ApiError(404, 'STATEMENT_NOT_FOUND', 'no statements for this account');
  }
  return periods[periods.length - 1];
}

function key(accountId: string, period: string): string {
  return `statements/${accountId}/${period}.pdf`;
}

/**
 * Ownership comes from fixtures locally. In the bank it is a call to bedrock-adapter
 * /customers/{id}/accounts, which is what the commented block in server.ts used to do before the
 * adapter's MTAI channel code for documents was withdrawn (PLAT-1610).
 */
function ownedAccounts(customerId: string): string[] {
  return fixtures()
    .accounts.filter((a) => a.customerId === customerId)
    .map((a) => a.accountId);
}
