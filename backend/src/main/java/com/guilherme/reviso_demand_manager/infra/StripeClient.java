package com.guilherme.reviso_demand_manager.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;

@Component
public class StripeClient {

    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;

    public StripeClient(
            @Value("${stripe.api-key:}") String apiKey,
            @Value("${stripe.base-url:https://api.stripe.com}") String baseUrl
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public String createCheckoutSession(String priceId, String successUrl, String cancelUrl, Map<String, String> metadata) throws Exception {
        var body = "mode=subscription" +
                "&price=" + priceId +
                "&quantity=1" +
                "&success_url=" + successUrl +
                "&cancel_url=" + cancelUrl;

        if (metadata != null) {
            for (var entry : metadata.entrySet()) {
                body += "&metadata[" + entry.getKey() + "]=" + entry.getValue();
            }
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/checkout/sessions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public String retrievePrice(String priceId) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/prices/" + priceId))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public String retrieveSubscription(String subscriptionId) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/subscriptions/" + subscriptionId))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
