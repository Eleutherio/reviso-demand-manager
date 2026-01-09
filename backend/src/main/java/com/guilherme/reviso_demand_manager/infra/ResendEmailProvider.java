package com.guilherme.reviso_demand_manager.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ResendEmailProvider implements EmailProvider {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String from;
    private final String baseUrl;
    private final Duration timeout;

    public ResendEmailProvider(
        @Value("${resend.api-key:}") String apiKey,
        @Value("${resend.from:}") String from,
        @Value("${resend.base-url:https://api.resend.com}") String baseUrl,
        @Value("${resend.timeout-seconds:10}") long timeoutSeconds
    ) {
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.from = from;
        this.baseUrl = baseUrl;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(this.timeout)
            .build();
    }

    @Override
    public EmailSendResult send(EmailMessage message) {
        if (apiKey == null || apiKey.isBlank()) {
            return new EmailSendResult(EmailSendStatus.FAILED, null, "Resend API key nao configurada");
        }
        if (from == null || from.isBlank()) {
            return new EmailSendResult(EmailSendStatus.FAILED, null, "Remetente nao configurado");
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                "from", from,
                "to", List.of(message.toEmail()),
                "subject", message.subject(),
                "text", message.body()
            ));
        } catch (IOException ex) {
            return new EmailSendResult(EmailSendStatus.FAILED, null, "Falha ao montar payload de email");
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(normalizeBaseUrl(baseUrl) + "/emails"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .timeout(timeout)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return new EmailSendResult(EmailSendStatus.SENT, extractId(response.body()), null);
            }
            if (statusCode == 402 || statusCode == 429) {
                return new EmailSendResult(EmailSendStatus.QUOTA, null, "Quota excedida");
            }
            if (statusCode >= 500) {
                return new EmailSendResult(EmailSendStatus.RETRY, null, "Falha temporaria no provedor");
            }
            return new EmailSendResult(EmailSendStatus.FAILED, null, "Erro ao enviar email");
        } catch (IOException ex) {
            return new EmailSendResult(EmailSendStatus.RETRY, null, "Falha de comunicacao com provedor");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new EmailSendResult(EmailSendStatus.RETRY, null, "Envio interrompido");
        }
    }

    private String extractId(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode idNode = node.get("id");
            if (idNode != null && idNode.isTextual()) {
                return idNode.asText();
            }
        } catch (IOException ex) {
            return null;
        }
        return null;
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) return "https://api.resend.com";
        if (value.endsWith("/")) return value.substring(0, value.length() - 1);
        return value;
    }
}
