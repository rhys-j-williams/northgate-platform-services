import { Module } from '@nestjs/common';
import { KeystoneJwtService } from './keystone-jwt.service';
import { JwtAuthGuard } from './jwt-auth.guard';

@Module({
  providers: [KeystoneJwtService, JwtAuthGuard],
  exports: [KeystoneJwtService, JwtAuthGuard],
})
export class AuthModule {}
