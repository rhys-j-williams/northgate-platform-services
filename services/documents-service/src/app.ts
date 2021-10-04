import express, { Express } from 'express';
import { correlationMiddleware } from './common/correlation';
import { errorHandler } from './common/api-error';
import { requireJwt } from './auth/jwt';
import { LocalDiskObjectStore, ObjectStore } from './store/object-store';
import { StatementsApiClient } from './clients/statements-api.client';
import { statementsRouter } from './routes/statements';
import { taxRouter } from './routes/tax';
import { disclosuresRouter } from './routes/disclosures';
import { config } from './config';

export interface Deps {
  store?: ObjectStore;
  statements?: StatementsApiClient;
}

export function createApp(deps: Deps = {}): Express {
  const store = deps.store ?? new LocalDiskObjectStore();
  const statements = deps.statements ?? new StatementsApiClient();
  const app = express();
  app.disable('x-powered-by');
  app.use(correlationMiddleware);
  app.use((req, res, next) => {
    const origin = req.header('origin');
    if (origin && config.corsOrigins.includes(origin)) {
      res.setHeader('Access-Control-Allow-Origin', origin);
      res.setHeader('Access-Control-Allow-Headers', 'Authorization, X-Correlation-Id');
      res.setHeader('Access-Control-Expose-Headers', 'X-Correlation-Id, X-Disclosure-Version, Content-Disposition');
    }
    if (req.method === 'OPTIONS') {
      res.sendStatus(204);
      return;
    }
    next();
  });

  app.get('/health', (_req, res) => res.json({ status: 'UP', service: config.serviceName }));
  app.get('/health/ready', (_req, res) => res.json({ status: 'UP', objectStore: config.objectStoreRoot, authMode: config.authMode }));

  // /documents/v1 is the published contract. /api/v1 is what retail-web's proxy.conf and the
  // estate smoke test still call; retail-web was written against the BFF prefix and nobody has
  // moved it (MOL-2981). Both prefixes mount the same routers.
  for (const prefix of ['/documents/v1', '/api/v1']) {
    app.use(prefix, disclosuresRouter());
    app.use(prefix, requireJwt, statementsRouter(store, statements));
    app.use(prefix, requireJwt, taxRouter(store));
  }

  app.use(errorHandler);
  return app;
}
