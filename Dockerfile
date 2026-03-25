ARG KEYCLOAK_VERSION=26.5.4

# --- Stage 1: Build PII Data Encryption provider ---
FROM maven:3.8.7-openjdk-18-slim AS keycloak-pii-data-encryption
ARG KEYCLOAK_VERSION
WORKDIR /app
RUN apt-get update && apt-get install -y git && apt-get clean
RUN git clone https://github.com/MLukman/Keycloak-PII-Data-Encryption-Provider.git .
RUN mvn clean package -Dkeycloak.version=$KEYCLOAK_VERSION

# --- Stage 2: Build Document Number authenticator SPI ---
FROM maven:3.8.7-openjdk-18-slim AS document-number-authenticator
ARG KEYCLOAK_VERSION
WORKDIR /app
COPY keycloak-document-number-authenticator/pom.xml .
COPY keycloak-document-number-authenticator/src ./src
RUN mvn clean package -Dkeycloak.version=$KEYCLOAK_VERSION

# --- Stage 3: Keycloak with custom providers and config ---
FROM quay.io/keycloak/keycloak:$KEYCLOAK_VERSION

# Add PII encryption provider
COPY --from=keycloak-pii-data-encryption /app/target/*.jar /opt/keycloak/providers/

# Add Document Number authenticator SPI
COPY --from=document-number-authenticator /app/target/*.jar /opt/keycloak/providers/

# Add custom configuration
RUN mkdir -p /opt/keycloak/conf
RUN echo "proxy-headers=xforwarded" >> /opt/keycloak/conf/keycloak.conf \
    && echo "http-enabled=true" >> /opt/keycloak/conf/keycloak.conf \
    && echo "proxy=edge" >> /opt/keycloak/conf/keycloak.conf \
    && echo "health-enabled=true" >> /opt/keycloak/conf/keycloak.conf \
    && echo "hostname-strict=false" >> /opt/keycloak/conf/keycloak.conf \
    && echo "proxy-address-forwarding=true" >> /opt/keycloak/conf/keycloak.conf

# Build optimized Keycloak
RUN /opt/keycloak/bin/kc.sh build \
    --db=postgres \
    --features="declarative-ui" \
    --spi-user-provider=jpa-encrypted
