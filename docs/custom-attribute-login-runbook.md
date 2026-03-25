# Custom Attribute Login — Runbook

Login to Keycloak using a custom user attribute (e.g. RUT/DNI, document number) instead of the default username or email.

## Prerequisites

- Keycloak 26.x running with Postgres
- Docker + Docker Compose
- Maven + JDK 17 (handled inside Docker build)

---

## Part 1: Build the Custom Authenticator SPI

The SPI extends Keycloak's built-in `UsernamePasswordForm` to look up users by a custom attribute.

### Project structure

```
keycloak-document-number-authenticator/
├── pom.xml
└── src/main/
    ├── java/com/ripley/keycloak/
    │   ├── DocumentNumberAuthenticator.java
    │   └── DocumentNumberAuthenticatorFactory.java
    └── resources/META-INF/services/
        └── org.keycloak.authentication.AuthenticatorFactory
```

### Key files

**`DocumentNumberAuthenticator.java`** — extends `UsernamePasswordForm` and overrides `validateForm` to authenticate users by the `document_number` attribute. The flow:

1. Reads the document number from the login form's username field
2. Records it for event logging and session tracking
3. Looks up the user by the `document_number` custom attribute
4. Rejects disabled or brute-force-locked accounts
5. Validates the submitted password against the matched user's credentials

```java
UserModel user = context.getSession().users()
        .searchForUserByUserAttributeStream(context.getRealm(), DOCUMENT_NUMBER_ATTRIBUTE, documentNumber)
        .findFirst()
        .orElse(null);
```

**`DocumentNumberAuthenticatorFactory.java`** — registers the SPI under provider ID `document-number-authenticator` with display name **"RUT/DNI Username Password Form"** visible in the admin console.

**`META-INF/services/org.keycloak.authentication.AuthenticatorFactory`** — service loader file pointing to the factory class.

### Dockerfile integration

The multi-stage Dockerfile builds the authenticator JAR and copies it into `/opt/keycloak/providers/`. A `KEYCLOAK_VERSION` build arg keeps the SPI and base image in sync:

```dockerfile
ARG KEYCLOAK_VERSION=26.5.4

FROM maven:3.8.7-openjdk-18-slim AS document-number-authenticator
ARG KEYCLOAK_VERSION
WORKDIR /app
COPY keycloak-document-number-authenticator/pom.xml .
COPY keycloak-document-number-authenticator/src ./src
RUN mvn clean package -Dkeycloak.version=$KEYCLOAK_VERSION

FROM quay.io/keycloak/keycloak:$KEYCLOAK_VERSION
COPY --from=document-number-authenticator /app/target/*.jar /opt/keycloak/providers/
```

The final stage also runs `kc.sh build` with `--db=postgres` and any required features/SPIs to produce an optimized Keycloak image.

Rebuild and restart:

```bash
docker compose build && docker compose up -d
```

---

## Part 2: Configure the Realm

### 1. Create a new realm

- Admin console > realm dropdown > **Create realm**
- Name it (e.g. `ripley`)

### 2. Add the custom attribute to User Profile

- **Realm settings > User profile > Create attribute**
- Attribute name: `document_number`
- Display name: `Document Number` (or `RUT/DNI`)
- Required: on
- Permissions: user can view and edit
- Optionally add a regex validator for your document format

### 3. Create a client

- **Clients > Create client**
- Client ID: your app name (e.g. `react-rut-app`)
- Client authentication: off (public, for SPAs)
- Valid redirect URIs: `http://localhost:5173/*`
- Valid post logout redirect URIs: `http://localhost:5173/*`
- Web origins: `http://localhost:5173`

### 4. Duplicate and configure the authentication flow

1. **Authentication > Flows > browser** > click the three-dot menu > **Duplicate** > name it (e.g. `browser-document-number`)
2. Open the duplicated flow
3. Find the **Username Password Form** execution step and **delete** it
4. Click **Add step** > search for **"RUT/DNI Username Password Form"** (the SPI display name)
5. Add it and set requirement to **Required**

### 5. Bind the custom flow

- Back in **Authentication > Flows**, find your duplicated flow
- Click the **three-dot menu** next to it > **Bind flow** > select **Browser flow**

### 6. Change the login form label

The login form still shows "Username or email" by default. Override it:

1. **Realm settings > Localization**
2. Enable **Internationalization**, add your locale (e.g. English)
3. Save, then switch to the **Realm overrides** sub-tab
4. Select the locale (e.g. English)
5. Click **Add translation**
6. Key: `usernameOrEmail` — Value: `Document Number (RUT/DNI)`
7. Save

### 7. Add a protocol mapper for the token claim

Expose the attribute in the token so your app can read it:

1. **Client scopes > profile > Mappers > Add mapper > By configuration**
2. Type: **User Attribute**
3. Name: `document_number`
4. User Attribute: `document_number`
5. Token Claim Name: `document_number`
6. Claim JSON Type: String
7. Add to ID token / Access token / Userinfo: all on

### 8. Create a test user

1. **Users > Add user**
2. Fill in username, email, first name, last name
3. Set the `document_number` attribute (e.g. `12.345.678-5`)
4. **Credentials** tab > set a password > toggle off "Temporary"
5. **Required user actions** > remove "Update Profile" if present (to avoid being forced to re-fill the profile on first login)

---

## Part 3: Test

1. Open your app and trigger login
2. The Keycloak login form should show "Document Number (RUT/DNI)" as the label
3. Enter the document number and password
4. After login, the token should contain a `document_number` claim alongside the standard `preferred_username` and `email`

---

## Adapting for Other Attributes

This pattern is generic. To use a different attribute:

1. Change `DOCUMENT_NUMBER_ATTRIBUTE` in the authenticator Java class to your attribute name
2. Update the factory display name and provider ID
3. Add the attribute in Keycloak's User Profile
4. Override the `usernameOrEmail` localization key to match your label
5. Create a protocol mapper for the new attribute
