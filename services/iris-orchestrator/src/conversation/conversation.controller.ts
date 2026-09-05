import { Body, Controller, Get, Headers, Param, Post } from '@nestjs/common';
import { ConversationService } from './conversation.service';
import { CurrentPrincipal } from '../auth/current-principal.decorator';
import { Principal } from '../auth/principal';
import { MessageDto } from './message.dto';

@Controller('sessions')
export class ConversationController {
  constructor(private readonly conversations: ConversationService) {}

  @Post()
  start(@CurrentPrincipal() principal: Principal) {
    return this.conversations.start(principal);
  }

  @Post(':sessionId/messages')
  message(
    @CurrentPrincipal() principal: Principal,
    @Param('sessionId') sessionId: string,
    @Body() body: MessageDto,
    @Headers('authorization') authorization: string,
  ) {
    return this.conversations.message(principal, sessionId, body.text, authorization.slice(7).trim());
  }

  @Get(':sessionId/transcript')
  transcript(@CurrentPrincipal() principal: Principal, @Param('sessionId') sessionId: string) {
    return this.conversations.transcript(principal, sessionId);
  }
}
