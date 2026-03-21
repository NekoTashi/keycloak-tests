import { Controller, Get, Request, UseGuards } from '@nestjs/common';
import { KeycloakGuard } from '../auth/keycloak.guard';
import { KeycloakTokenPayload } from '../auth/keycloak.strategy';

@Controller('protected')
export class ProtectedController {
  @Get()
  @UseGuards(KeycloakGuard)
  getProfile(@Request() req: { user: KeycloakTokenPayload }): KeycloakTokenPayload {
    return req.user;
  }
}
