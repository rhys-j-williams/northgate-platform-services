import { Body, Controller, Get, Param, Post } from '@nestjs/common';
import { IsISO8601, IsOptional, IsString } from 'class-validator';
import { CardsService } from './cards.service';
import { CurrentPrincipal } from '../auth/current-principal.decorator';
import { Principal } from '../auth/principal';

export class TravelNoticeDto {
  @IsISO8601()
  until!: string;

  @IsOptional()
  @IsString()
  destination?: string;
}

@Controller('cards')
export class CardsController {
  constructor(private readonly cards: CardsService) {}

  @Get()
  list(@CurrentPrincipal() principal: Principal) {
    return this.cards.list(principal);
  }

  @Post(':cardId/lock')
  lock(@CurrentPrincipal() principal: Principal, @Param('cardId') cardId: string) {
    return this.cards.setLocked(principal, cardId, true);
  }

  @Post(':cardId/unlock')
  unlock(@CurrentPrincipal() principal: Principal, @Param('cardId') cardId: string) {
    return this.cards.setLocked(principal, cardId, false);
  }

  @Post(':cardId/travel-notice')
  travelNotice(@CurrentPrincipal() principal: Principal, @Param('cardId') cardId: string, @Body() dto: TravelNoticeDto) {
    return this.cards.travelNotice(principal, cardId, dto.until);
  }
}
