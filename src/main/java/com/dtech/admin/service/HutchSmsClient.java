package com.dtech.admin.service;

import com.dtech.admin.dto.api.MessageResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@Log4j2
public class HutchSmsClient {
    private static final Duration TOKEN_CACHE_DURATION = Duration.ofHours(23);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String mask;
    private final String campaignName;

    private String accessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public HutchSmsClient(
            RestTemplate restTemplate,
            @Value("${hutch.sms.base-url:https://bsms.hutch.lk}") String baseUrl,
            @Value("${hutch.sms.username:}") String username,
            @Value("${hutch.sms.password:}") String password,
            @Value("${hutch.sms.mask:}") String mask,
            @Value("${hutch.sms.campaign-name:WeCare}") String campaignName) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.username = username;
        this.password = password;
        this.mask = mask;
        this.campaignName = campaignName;
    }

    public MessageResponseDTO send(String mobile, String content) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)
                || !StringUtils.hasText(mask) || !StringUtils.hasText(campaignName)) {
            log.error("Hutch SMS configuration is incomplete");
            return failure("SMS provider is not configured");
        }

        String number = normalizeMobile(mobile);
        if (number == null) {
            log.warn("Hutch SMS destination has an invalid mobile number");
            return failure("Invalid mobile number");
        }

        try {
            String token = currentToken();
            try {
                return submit(number, content, token);
            } catch (HttpClientErrorException.Unauthorized unauthorized) {
                invalidateToken();
                // Hutch documents HTTP 401 as an expired token. Retry only once.
                return submit(number, content, currentToken());
            }
        } catch (RestClientException e) {
            log.error("Hutch SMS request failed: {}", e.getClass().getSimpleName());
            return failure("SMS provider request failed");
        } catch (IllegalStateException e) {
            log.error("Hutch SMS authentication failed: {}", e.getMessage());
            return failure("SMS provider authentication failed");
        }
    }

    private MessageResponseDTO submit(String number, String content, String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        Map<String, Object> body = Map.of(
                "campaignName", campaignName,
                "mask", mask,
                "numbers", number,
                "content", content);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + "/api/sendsms", HttpMethod.POST,
                new HttpEntity<>(body, headers), JsonNode.class);
        JsonNode result = response.getBody();
        if (response.getStatusCode().is2xxSuccessful() && result != null
                && StringUtils.hasText(result.path("serverRef").asText(null))) {
            log.info("Hutch accepted SMS request serverRef={}", result.path("serverRef").asText());
            return MessageResponseDTO.builder().success(true).build();
        }
        log.warn("Hutch SMS response did not contain serverRef");
        return failure("SMS provider did not accept the message");
    }

    private synchronized String currentToken() {
        if (StringUtils.hasText(accessToken) && Instant.now().isBefore(tokenExpiresAt)) {
            return accessToken;
        }
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + "/api/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", username, "password", password), jsonHeaders()),
                JsonNode.class);
        JsonNode body = response.getBody();
        String token = body == null ? null : body.path("accessToken").asText(null);
        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(token)) {
            throw new IllegalStateException("Login response did not contain an access token");
        }
        accessToken = token;
        tokenExpiresAt = Instant.now().plus(TOKEN_CACHE_DURATION);
        return accessToken;
    }

    private synchronized void invalidateToken() {
        accessToken = null;
        tokenExpiresAt = Instant.EPOCH;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("X-API-VERSION", "v1");
        return headers;
    }

    static String normalizeMobile(String mobile) {
        if (mobile == null) return null;
        String number = mobile.trim().replaceAll("[\\s-]", "");
        if (number.startsWith("+")) number = number.substring(1);
        if (number.matches("07\\d{8}")) return "94" + number.substring(1);
        if (number.matches("7\\d{8}")) return "94" + number;
        if (number.matches("947\\d{8}")) return number;
        return null;
    }

    private MessageResponseDTO failure(String message) {
        return MessageResponseDTO.builder().success(false).message(message).build();
    }
}
