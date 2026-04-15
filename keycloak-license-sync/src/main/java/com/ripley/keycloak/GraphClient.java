package com.ripley.keycloak;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class GraphClient {

    private final String tenantId, clientId, clientSecret;
    private String token;
    private long tokenExpiry;

    public GraphClient(String tenantId, String clientId, String clientSecret) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public Set<String> getLicenses(String userPrincipalName) throws Exception {
        var res = graphGet("/users/" + encode(userPrincipalName) + "/licenseDetails");
        var skus = new HashSet<String>();
        for (var el : JsonParser.parseString(res).getAsJsonObject().getAsJsonArray("value")) {
            skus.add(el.getAsJsonObject().get("skuId").getAsString());
        }
        return skus;
    }

    public void syncLicenses(String userPrincipalName,
                             Set<String> toAdd,
                             Set<String> toRemove) throws Exception {
        var addArr = new JsonArray();
        for (var sku : toAdd) {
            var obj = new JsonObject();
            obj.addProperty("skuId", sku);
            obj.add("disabledPlans", new JsonArray());
            addArr.add(obj);
        }

        var removeArr = new JsonArray();
        toRemove.forEach(removeArr::add);

        var body = new JsonObject();
        body.add("addLicenses", addArr);
        body.add("removeLicenses", removeArr);

        graphPost("/users/" + encode(userPrincipalName) + "/assignLicense", body);
    }

    private String graphGet(String path) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.microsoft.com/v1.0" + path))
                .header("Authorization", "Bearer " + getToken())
                .GET().build();
        var res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("GET failed: " + res.body());
        return res.body();
    }

    private void graphPost(String path, JsonObject body) throws Exception {
        var req = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.microsoft.com/v1.0" + path))
                .header("Authorization", "Bearer " + getToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        var res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("POST failed: " + res.body());
    }

    private synchronized String getToken() throws Exception {
        if (token != null && System.currentTimeMillis() < tokenExpiry) return token;

        var form = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&scope=" + encode("https://graph.microsoft.com/.default")
                + "&grant_type=client_credentials";

        var req = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();

        var json = JsonParser.parseString(
                HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()).body()
        ).getAsJsonObject();

        token = json.get("access_token").getAsString();
        tokenExpiry = System.currentTimeMillis() + (json.get("expires_in").getAsLong() * 1000) - 60_000;
        return token;
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
