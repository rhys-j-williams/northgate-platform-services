import { Body, Controller, Get, HttpCode, Param, Post, Query } from '@nestjs/common';
import { IsInt, IsISO8601, IsOptional, IsString, Length, Matches, Min } from 'class-validator';
import { ApprovalsService } from './approvals.service';
import { Approval } from './approval';
import { CurrentPrincipal } from '../auth/current-principal.decorator';
import { Principal } from '../auth/principal';

export class SubmitPaymentDto {
  @IsString()
  @Matches(/^ACC-\d{9}$/)
  fromAccountId!: string;

  @IsInt()
  @Min(1)
  amountMinor!: number;

  @IsString()
  @Length(2, 80)
  beneficiary!: string;

  @IsOptional()
  @IsString()
  @Length(0, 35)
  reference?: string;

  @IsOptional()
  @IsISO8601()
  valueDate?: string;
}

export class RejectDto {
  @IsString()
  @Length(3, 200)
  reason!: string;
}

@Controller('approvals')
export class ApprovalsController {
  constructor(private readonly approvals: ApprovalsService) {}

  @Get()
  list(@CurrentPrincipal() principal: Principal, @Query('status') status?: Approval['status']) {
    return this.approvals.list(principal, status);
  }

  @Post('payments')
  @HttpCode(201)
  submit(@CurrentPrincipal() principal: Principal, @Body() dto: SubmitPaymentDto) {
    return this.approvals.submitPayment(principal, dto);
  }

  @Post(':approvalId/approve')
  approve(@CurrentPrincipal() principal: Principal, @Param('approvalId') approvalId: string) {
    return this.approvals.approve(principal, approvalId);
  }

  @Post(':approvalId/reject')
  reject(@CurrentPrincipal() principal: Principal, @Param('approvalId') approvalId: string, @Body() dto: RejectDto) {
    return this.approvals.reject(principal, approvalId, dto.reason);
  }
}
