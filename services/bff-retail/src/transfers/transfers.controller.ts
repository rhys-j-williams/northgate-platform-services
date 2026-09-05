import { Body, Controller, Get, HttpCode, Post } from '@nestjs/common';
import { TransfersService } from './transfers.service';
import { CreateTransferDto } from './transfer.dto';
import { CurrentPrincipal } from '../auth/current-principal.decorator';
import { Principal } from '../auth/principal';

@Controller('transfers')
export class TransfersController {
  constructor(private readonly transfers: TransfersService) {}

  @Post()
  @HttpCode(201)
  create(@CurrentPrincipal() principal: Principal, @Body() dto: CreateTransferDto) {
    return this.transfers.create(principal, dto);
  }

  @Get('limits')
  limits(@CurrentPrincipal() principal: Principal) {
    return this.transfers.limits(principal);
  }
}
