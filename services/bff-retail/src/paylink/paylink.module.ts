import { Module } from '@nestjs/common';
import { PayLinkController } from './paylink.controller';

@Module({ controllers: [PayLinkController] })
export class PayLinkModule {}
