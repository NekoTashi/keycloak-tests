package com.ripley.keycloak;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class LicenseSyncFactory implements EventListenerProviderFactory {

    private GraphClient graph;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new LicenseSyncProvider(session, graph);
    }

    @Override
    public void init(Config.Scope config) {
        graph = new GraphClient(
                config.get("tenantId"),
                config.get("clientId"),
                config.get("clientSecret")
        );
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}

    @Override
    public String getId() { return "license-sync"; }
}
