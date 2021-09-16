import { Body, Controller, Get, HttpCode, Post } from '@nestjs/common';
import { IsBoolean, IsInt, IsOptional, IsString, Length, Max, Min } from 'class-validator';
import { PayLinkClient } from '../clients/partner.clients';
import { CurrentPrincipal } from '../auth/current-principal.decorator';
import { Principal } from '../auth/principal';
import { ApiException } from '../common/api-error';

export class PayLinkPaymentDto {
  @IsString()
  contactId!: string;

  @IsInt()
  @Min(100)
  @Max(250_000)
  amountMinor!: number;

  @IsOptional()
  @IsString()
  @Length(0, 40)
  memo?: string;

  @IsOptional()
  @IsBoolean()
  request?: boolean;
}

@Controller('paylink')
export class PayLinkController {
  constructor(private readonly paylink: PayLinkClient) {}

  @Get('contacts')
  contacts(@CurrentPrincipal() principal: Principal) {
    return this.paylink.contacts(principal.customerId);
  }

  @Get('activity')
  activity(@CurrentPrincipal() principal: Principal) {
    return this.paylink.activity(principal.customerId);
  }

  @Post('payments')
  @HttpCode(201)
  async pay(@CurrentPrincipal() principal: Principal, @Body() dto: PayLinkPaymentDto) {
    const contacts = await this.paylink.contacts(principal.customerId);
    const contact = contacts.find((c) => c.contactId === dto.contactId);
    if (!contact) {
      throw ApiException.notFound('PAYLINK_CONTACT_NOT_FOUND', 'contact not in address book');
    }
    if (!contact.verified && !dto.request) {
      throw ApiException.conflict('PAYLINK_CONTACT_UNVERIFIED', 'sending to an unverified contact is blocked');
    }
    // request money is behind the Semaphore flag paylink_request_money in retail-web; the BFF does not gate it (MOL-2977)
    return this.paylink.send(principal.customerId, dto.contactId, dto.amountMinor, dto.memo, dto.request === true);
  }
}
