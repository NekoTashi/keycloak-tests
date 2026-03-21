import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy, StrategyOptionsWithoutRequest } from 'passport-jwt';
import { passportJwtSecret } from 'jwks-rsa';

export interface KeycloakTokenPayload {
  sub: string;
  email_verified: boolean;
  name: string;
  preferred_username: string;
  given_name: string;
  family_name: string;
  email: string;
  realm_access: { roles: string[] };
}

@Injectable()
export class KeycloakStrategy extends PassportStrategy(Strategy, 'keycloak') {
  constructor() {
    const realm = process.env.KEYCLOAK_REALM ?? 'test';
    const issuer = `${process.env.KEYCLOAK_URL ?? 'http://localhost:8080'}/realms/${realm}`;

    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      secretOrKeyProvider: passportJwtSecret({
        cache: true,
        rateLimit: true,
        jwksRequestsPerMinute: 5,
        jwksUri: `${issuer}/protocol/openid-connect/certs`,
      }),
      issuer,
      algorithms: ['RS256'],
    } satisfies StrategyOptionsWithoutRequest);
  }

  validate(payload: KeycloakTokenPayload): KeycloakTokenPayload {
    return payload;
  }
}
