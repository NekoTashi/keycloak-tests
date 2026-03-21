import { Module } from '@nestjs/common';
import { AuthModule } from './auth/auth.module';
import { ProtectedModule } from './protected/protected.module';

@Module({
  imports: [AuthModule, ProtectedModule],
})
export class AppModule {}
