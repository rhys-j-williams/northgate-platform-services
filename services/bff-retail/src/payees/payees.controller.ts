import { Body, Controller, Get, HttpCode, Post } from '@nestjs/common';
import { IsIn, IsString, Length, Matches } from 'class-validator';
import { Payee } from '@meridian/domain-fixtures';
import { FixturesService } from '../clients/fixtures.service';
import { TriScoreClient } from '../clients/partner.clients';
import { CacheService } from '../cache/cache.service';
import { CurrentPrincipal } from '../auth/current-principal.decorator';
import { Principal } from '../auth/principal';

export class AddPayeeDto {
  @IsString()
  @Length(2, 60)
  name!: string;

  @IsString()
  @Length(0, 30)
  nickname!: string;

  @IsString()
  @Matches(/^\d{4}$/)
  accountNumberLastFour!: string;

  @IsString()
  @Matches(/^\d{9}$/)
  routingNumber!: string;

  @IsIn(['bill-pay', 'external-transfer'])
  type!: 'bill-pay' | 'external-transfer';
}

/**
 * Bill pay payees. Fixture backed plus whatever was added this session (cache, 30 days). The bill
 * pay processor integration was descoped from the 2023 replatform; PLAT-1602 is still open.
 */
@Controller('payees')
export class PayeesController {
  constructor(private readonly fixtures: FixturesService, private readonly triscore: TriScoreClient, private readonly cache: CacheService) {}

  @Get()
  async list(@CurrentPrincipal() principal: Principal): Promise<Payee[]> {
    const added = (await this.cache.get<Payee[]>(this.key(principal))) ?? [];
    const fixture = this.fixtures.get().payees.filter((p) => p.customerId === principal.customerId && p.type !== 'paylink');
    return [...added, ...fixture];
  }

  @Post()
  @HttpCode(201)
  async add(@CurrentPrincipal() principal: Principal, @Body() dto: AddPayeeDto): Promise<Payee> {
    const verification = await this.triscore.verifyPayee(dto.name, dto.routingNumber);
    const payee: Payee = {
      payeeId: `PAY-${Date.now().toString().slice(-9)}`,
      customerId: principal.customerId,
      name: dto.name,
      nickname: dto.nickname || dto.name,
      accountNumberLastFour: dto.accountNumberLastFour,
      routingNumber: dto.routingNumber,
      type: dto.type,
      verified: verification.verified,
      addedAt: new Date().toISOString(),
    };
    const added = (await this.cache.get<Payee[]>(this.key(principal))) ?? [];
    await this.cache.set(this.key(principal), [payee, ...added], 30 * 86_400);
    return payee;
  }

  private key(principal: Principal): string {
    return `payees:${principal.customerId}`;
  }
}
