import 'reflect-metadata';
import { ValidationPipe, Logger } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { ConfigService } from '@nestjs/config';
import { AppModule } from './app.module';
import { AllExceptionFilter } from './common/filters/all-exception.filter';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, {
    logger: ['error', 'warn', 'log'],
  });

  const config = app.get(ConfigService);
  const prefix = config.get<string>('API_PREFIX', 'api');
  app.setGlobalPrefix(prefix, { exclude: ['health', 'health/(.*)'] });
  app.enableCors({ origin: true, credentials: true });

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      transform: true,
      forbidNonWhitelisted: false,
      transformOptions: { enableImplicitConversion: true },
    }),
  );
  app.useGlobalFilters(new AllExceptionFilter());

  const port = Number(config.get('PORT', 8080));
  await app.listen(port, '0.0.0.0');
  new Logger('Bootstrap').log(`server listening on http://127.0.0.1:${port}/${prefix}`);
}

void bootstrap();
