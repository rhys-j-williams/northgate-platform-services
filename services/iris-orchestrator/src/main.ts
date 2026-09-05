import 'reflect-metadata';
import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { AppModule } from './app.module';
import { StructuredLogger } from './common/logger';
import { ApiExceptionFilter } from './common/api-exception.filter';
import { config } from './config';

async function bootstrap(): Promise<void> {
  const logger = new StructuredLogger('bootstrap');
  const app = await NestFactory.create(AppModule, { logger, cors: { origin: config.corsOrigins } });
  app.setGlobalPrefix('iris/v1', { exclude: ['health', 'health/ready'] });
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  app.useGlobalFilters(new ApiExceptionFilter());
  app.enableShutdownHooks();
  await app.listen(config.port);
  logger.log(`iris-orchestrator listening on ${config.port} (auth=${config.authMode}, bff=${config.bffRetailUrl})`);
}

bootstrap().catch((err) => {
  // eslint-disable-next-line no-console
  console.error(JSON.stringify({ event: 'bootstrap failed', severity: 'ERROR', error: String(err) }));
  process.exit(1);
});
