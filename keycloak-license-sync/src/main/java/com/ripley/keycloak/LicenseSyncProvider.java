package com.ripley.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class LicenseSyncProvider implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(LicenseSyncProvider.class);
    private static final String ATTR = "ms_licenses";

    private final KeycloakSession session;
    private final GraphClient graph;

    public LicenseSyncProvider(KeycloakSession session, GraphClient graph) {
        this.session = session;
        this.graph = graph;
    }

    @Override
    public void onEvent(Event event) {
        var type = event.getType();
        if (type != EventType.LOGIN
                && type != EventType.REGISTER
                && type != EventType.UPDATE_PROFILE) return;

        try {
            var user = session.users()
                    .getUserById(session.getContext().getRealm(), event.getUserId());
            if (user == null || user.getEmail() == null) return;

            var desired  = parseAttr(user.getFirstAttribute(ATTR));
            var current  = graph.getLicenses(user.getEmail());

            var toAdd    = diff(desired, current);
            var toRemove = diff(current, desired);

            if (toAdd.isEmpty() && toRemove.isEmpty()) return;

            graph.syncLicenses(user.getEmail(), toAdd, toRemove);
            LOG.infof("[license-sync] %s: +%s -%s", user.getEmail(), toAdd, toRemove);

        } catch (Exception e) {
            LOG.errorf(e, "[license-sync] failed for user %s", event.getUserId());
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRep) {}

    @Override
    public void close() {}

    private Set<String> parseAttr(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private Set<String> diff(Set<String> a, Set<String> b) {
        var result = new HashSet<>(a);
        result.removeAll(b);
        return result;
    }
}
