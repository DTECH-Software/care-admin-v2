/**
 * User: Himal_J
 * Date: 4/30/2025
 * Time: 11:53 AM
 * <p>
 */

package com.dtech.admin.service;

import com.dtech.admin.dto.api.MessageResponseDTO;
import com.dtech.admin.enums.MessageType;
import com.dtech.admin.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;

@Service
@Log4j2
@RequiredArgsConstructor
public class MessageService {

    @Autowired
    private final NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final RestTemplate restTemplate;

    @Value("${message.uri}")
    private String messageURI;

    @Value("${message.api.key}")
    private String apiKey;

    @Async
    public void sendMessageAsync(MessageType messageType, String message, String otherMessage, String mobile) {
        try {
            sendMessage(messageType, message, otherMessage, mobile);
        } catch (Exception e) {
            log.error("Async message sending failed for type {} and mobile {}", messageType, mobile, e);
        }
    }

    @Transactional
    public MessageResponseDTO sendMessage(MessageType messageType, String message,String otherMessage, String mobile) {
        try {
            log.info("Message service started {} {} {} ", messageType, message, mobile);

            return notificationTemplateRepository
                    .findByType(messageType).map((template) -> {

                        String formatMessage = MessageFormat.format(template.getMessageBody(), message,otherMessage);
                        HttpHeaders headers = new HttpHeaders();
                        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
                        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + apiKey);
                        headers.set("X-API-VERSION", "v1");

                        return sendJsonMessage(mobile, formatMessage, headers);
                    }).orElseGet(() -> {
                        log.info("Template {} not found", messageType.name());
                        return MessageResponseDTO.builder()
                                .success(false)
                                .message(messageSource.getMessage("val.notification.template.not.found", null, null)).build();
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private MessageResponseDTO sendJsonMessage(String mobile, String formatMessage, HttpHeaders headers) {
        String jsonPayload = "{\"to\":\"" + escapeJson(mobile) + "\",\"text\":\"" + escapeJson(formatMessage) + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
        log.info("Before send message raw JSON {}", jsonPayload);
        try {
            ResponseEntity<String> response = restTemplate.exchange(messageURI, HttpMethod.POST, entity, String.class);
            if (response.getBody() == null || response.getBody().isEmpty()) {
                log.error("Received empty response body from the API");
                return MessageResponseDTO.builder()
                        .success(false)
                        .message("No response body from the API").build();
            }
            log.info("After send message {}", response.toString());
            MessageResponseDTO responseState = getResponseState(response);
            responseState.setMessage(messageSource.getMessage("val.otp.send.success", null, null));
            return responseState;
        } catch (HttpStatusCodeException ex) {
            log.error("Failed to send message. Status: {}, Response body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return MessageResponseDTO.builder()
                    .success(false)
                    .message("Message sending failed: " + ex.getStatusCode())
                    .build();
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Transactional
    protected MessageResponseDTO getResponseState(ResponseEntity<String> response) {
        try {
            log.info("Response state for send otp: {}", response.getBody());
            HttpStatusCode statusCode = response.getStatusCode();

            boolean b = switch (statusCode) {
                case HttpStatus.OK -> true;
                default -> false;
            };

            MessageResponseDTO messageResponseDTO = new MessageResponseDTO();
            messageResponseDTO.setSuccess(b);
            return messageResponseDTO;

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}
