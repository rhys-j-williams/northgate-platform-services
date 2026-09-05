import { IsInt, IsISO8601, IsOptional, IsString, Length, Matches, Min } from 'class-validator';

export class CreateTransferDto {
  @IsString()
  @Length(8, 64)
  idempotencyKey!: string;

  @IsString()
  @Matches(/^ACC-\d{9}$/)
  fromAccountId!: string;

  @IsOptional()
  @IsString()
  @Matches(/^ACC-\d{9}$/)
  toAccountId?: string;

  @IsOptional()
  @IsString()
  toPayeeId?: string;

  @IsInt()
  @Min(1)
  amountMinor!: number;

  @IsOptional()
  @IsString()
  @Length(0, 60)
  memo?: string;

  @IsOptional()
  @IsISO8601()
  scheduledFor?: string;
}
