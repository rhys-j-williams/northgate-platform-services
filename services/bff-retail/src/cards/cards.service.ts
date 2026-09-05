import { Injectable } from '@nestjs/common';
import { Card, maskCardNumber } from '@meridian/domain-fixtures';
import { FixturesService } from '../clients/fixtures.service';
import { CacheService } from '../cache/cache.service';
import { ApiException } from '../common/api-error';
import { Principal } from '../auth/principal';

export interface CardDto {
  cardId: string;
  accountId: string;
  maskedNumber: string;
  network: string;
  expiry: string;
  status: string;
  contactlessEnabled: boolean;
  travelNoticeUntil?: string;
  digitalWallet: Card['digitalWallet'];
}

/**
 * Card controls. There is no card management system in the estate - in the bank this is the
 * card processor's API through the payments gateway - so state lives in the cache on top of the
 * fixture cards. Good enough for the demo; PLAT-1933 covers the real thing if it ever comes.
 */
@Injectable()
export class CardsService {
  constructor(private readonly fixtures: FixturesService, private readonly cache: CacheService) {}

  async list(principal: Principal): Promise<CardDto[]> {
    const cards = this.fixtures.get().cards.filter((c) => c.customerId === principal.customerId);
    return Promise.all(cards.map((c) => this.toDto(c)));
  }

  async setLocked(principal: Principal, cardId: string, locked: boolean): Promise<CardDto> {
    const card = this.owned(principal, cardId);
    if (card.status === 'replaced' || card.status === 'expired') {
      throw ApiException.conflict('CARD_NOT_ACTIVE', `card is ${card.status}`);
    }
    await this.cache.set(`card:${cardId}:status`, locked ? 'locked' : 'active', 7 * 86_400);
    return this.toDto(card);
  }

  async travelNotice(principal: Principal, cardId: string, until: string): Promise<CardDto> {
    const card = this.owned(principal, cardId);
    if (new Date(until).getTime() < Date.now()) {
      throw ApiException.badRequest('TRAVEL_NOTICE_IN_PAST', 'until must be in the future');
    }
    await this.cache.set(`card:${cardId}:travel`, until, 90 * 86_400);
    return this.toDto(card);
  }

  private owned(principal: Principal, cardId: string): Card {
    const card = this.fixtures.get().cards.find((c) => c.cardId === cardId && c.customerId === principal.customerId);
    if (!card) {
      throw ApiException.notFound('CARD_NOT_FOUND', `card ${cardId} not found`);
    }
    return card;
  }

  private async toDto(card: Card): Promise<CardDto> {
    const status = (await this.cache.get<string>(`card:${card.cardId}:status`)) ?? card.status;
    const travel = (await this.cache.get<string>(`card:${card.cardId}:travel`)) ?? card.travelNoticeUntil;
    return {
      cardId: card.cardId,
      accountId: card.accountId,
      maskedNumber: maskCardNumber(card.cardNumber),
      network: card.network,
      expiry: `${String(card.expiryMonth).padStart(2, '0')}/${String(card.expiryYear).slice(-2)}`,
      status,
      contactlessEnabled: card.contactlessEnabled,
      travelNoticeUntil: travel,
      digitalWallet: card.digitalWallet,
    };
  }
}
