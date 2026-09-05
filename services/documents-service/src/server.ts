import { createApp } from './app';
import { config } from './config';
import { log } from './common/logger';

const server = createApp().listen(config.port, () => {
  log('INFO', `documents-service listening on ${config.port} (auth=${config.authMode}, store=${config.objectStoreRoot}, statements=${config.statementsApiUrl})`);
});

// Account ownership used to come from the adapter. Kept for when PLAT-1610 gives us a channel code back.
// const ownership = axios.create({ baseURL: config.bedrockAdapterUrl, headers: { 'X-Channel': 'DOC' } });

for (const sig of ['SIGINT', 'SIGTERM'] as const) {
  process.on(sig, () => {
    server.close(() => process.exit(0));
  });
}
