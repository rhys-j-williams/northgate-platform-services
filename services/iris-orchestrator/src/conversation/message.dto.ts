import { IsString, Length } from 'class-validator';

export class MessageDto {
  @IsString()
  @Length(1, 500)
  text!: string;
}
