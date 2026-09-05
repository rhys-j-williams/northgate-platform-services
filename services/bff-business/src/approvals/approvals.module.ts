import { Module } from '@nestjs/common';
import { EntitlementsModule } from '../entitlements/entitlements.module';
import { ApprovalsController } from './approvals.controller';
import { ApprovalsService } from './approvals.service';

@Module({ imports: [EntitlementsModule], controllers: [ApprovalsController], providers: [ApprovalsService] })
export class ApprovalsModule {}
