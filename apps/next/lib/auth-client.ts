import { createAuthClient } from "better-auth/react";
import { genericOAuthClient } from "better-auth/client/plugins";

export const authClient = createAuthClient({
  plugins: [genericOAuthClient()],
});

export const { useSession, signOut } = authClient;

export function signInWithKeycloak() {
  return authClient.signIn.oauth2({
    providerId: "keycloak",
    callbackURL: "/protected",
  });
}
