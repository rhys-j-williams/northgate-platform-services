import { Controller, Get } from '@nestjs/common';
import { HandoffQueueService } from './handoff-queue.service';

/**
 * Local visibility of tickets held in memory. Behind the JWT guard like everything else; in the
 * bank this route is also blocked at the ingress (only Semaphore reads the queue, from Redis).
 */
@Controller('handoff')
export class HandoffController {
  constructor(private readonly queue: HandoffQueueService) {}

  @Get('queue')
  queueView() {
    return { mode: this.queue.mode, waiting: this.queue.depth(), tickets: this.queue.waiting() };
  }
}
