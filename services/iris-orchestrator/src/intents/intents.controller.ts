import { Controller, Get } from '@nestjs/common';
import { IntentLoader } from './intent-loader';
import { Public } from '../auth/jwt-auth.guard';

/** Read only view for the widget's quick reply seeding and for whoever is debugging the YAML. */
@Controller('intents')
export class IntentsController {
  constructor(private readonly loader: IntentLoader) {}

  @Public()
  @Get()
  list() {
    const file = this.loader.load();
    return {
      version: file.version,
      intents: file.intents.map((i) => ({ id: i.id, keywords: i.keywords.length, handoff: i.handoff ?? false, requiresAuth: i.requires_auth ?? false })),
    };
  }
}
