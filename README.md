# Keycloak Auth Tests

Three apps demonstrating different authentication patterns, all using **Keycloak** as the identity provider.

## Apps

### Nest — API-side JWT validation

- **Library:** `passport-jwt` + `jwks-rsa`
- **How it works:** The client sends a Bearer token in the `Authorization` header. Nest fetches Keycloak's public keys (JWKS endpoint) and validates the JWT signature and claims locally. No login flow lives here — it just verifies tokens that were obtained elsewhere.
- **Use case:** Backend API that trusts tokens issued by Keycloak. It never redirects users to log in; it just says "show me a valid token or get a 401."

### Next.js — Server-side OAuth2 (Authorization Code flow)

- **Library:** `better-auth` with `genericOAuth` plugin
- **How it works:** The Next.js server acts as an **OAuth2 client**. When a user clicks "login," the server redirects them to Keycloak. After login, Keycloak redirects back with an authorization code. The server exchanges that code for tokens **server-to-server** (using a client secret), then stores the session in an encrypted cookie. The browser never sees the raw tokens.
- **Use case:** Full-stack app where the server manages sessions and can protect pages with server-side checks before rendering.

### React (Vite SPA) — OIDC with PKCE (browser-only)

- **Library:** `react-oidc-context` / `oidc-client-ts`
- **How it works:** The React app redirects the user to Keycloak directly from the browser. After login, Keycloak redirects back with an authorization code. The browser exchanges the code for tokens using **PKCE** (no client secret needed, since there's no server to keep one safe). Tokens live in the browser.
- **Use case:** Pure single-page app with no backend. Auth happens entirely in the browser.

## Comparison

| | **Nest** | **Next.js** | **React SPA** |
|---|---|---|---|
| **Role** | Token verifier (API) | OAuth2 client (server) | OAuth2 client (browser) |
| **Login flow** | None — expects a token | Authorization Code + client secret | Authorization Code + PKCE |
| **Where tokens live** | Passed by the caller | Server-side (encrypted cookie) | Browser memory |
| **Client secret?** | No | Yes (server keeps it) | No (public client) |
| **Security model** | Stateless JWT validation | Server manages session | Browser manages tokens |

**In short:** Nest is a pure API guard, Next.js does the full login dance on the server (more secure, has a secret), and the React SPA does the login dance in the browser (no server, uses PKCE to compensate for having no secret).
