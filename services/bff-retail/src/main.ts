import 'reflect-metadata';
import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { AppModule } from './app.module';
import { StructuredLogger } from './common/logger';
import { CorrelationMiddleware } from './common/correlation.middleware';
import { ApiExceptionFilter } from './common/api-exception.filter';
import { config } from './config';

async function bootstrap(): Promise<void> {
  const logger = new StructuredLogger('bootstrap');
  const app = await NestFactory.create(AppModule, { logger, cors: { origin: config.corsOrigins } });
  app.setGlobalPrefix('api/v1', { exclude: ['health', 'health/ready'] });
  app.use(new CorrelationMiddleware().use);
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  app.useGlobalFilters(new ApiExceptionFilter());
  app.enableShutdownHooks();
  await app.listen(config.port);
  logger.log(`bff-retail listening on ${config.port} (auth=${config.authMode}, redis=${config.redisUrl})`);
}

bootstrap().catch((err) => {
  // eslint-disable-next-line no-console
  console.error(JSON.stringify({ event: 'bootstrap failed', severity: 'ERROR', error: String(err) }));
  process.exit(1);
});
