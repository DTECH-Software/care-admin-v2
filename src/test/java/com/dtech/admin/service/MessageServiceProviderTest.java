package com.dtech.admin.service;

import com.dtech.admin.dto.api.MessageResponseDTO;
import com.dtech.admin.enums.MessageType;
import com.dtech.admin.model.NotificationTemplate;
import com.dtech.admin.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class MessageServiceProviderTest {
    private NotificationTemplateRepository templates;
    private RestTemplate restTemplate;
    private HutchSmsClient hutchSmsClient;
    private MessageService service;

    @BeforeEach
    void setUp() {
        templates = mock(NotificationTemplateRepository.class);
        restTemplate = mock(RestTemplate.class);
        hutchSmsClient = mock(HutchSmsClient.class);
        service = new MessageService(templates, mock(MessageSource.class), restTemplate, hutchSmsClient);
        NotificationTemplate template = new NotificationTemplate();
        template.setMessageBody("Your OTP is {0}");
        when(templates.findByType(MessageType.SENT_OTP_PASSWORD)).thenReturn(Optional.of(template));
    }

    @Test
    void sendsAdminOtpThroughHutchWhenSelected() {
        ReflectionTestUtils.setField(service, "provider", "hutch");
        when(hutchSmsClient.send("0712345678", "Your OTP is 123456"))
                .thenReturn(MessageResponseDTO.builder().success(true).build());

        assertTrue(service.sendMessage(MessageType.SENT_OTP_PASSWORD, "123456", null, "0712345678")
                .isSuccess());

        verify(hutchSmsClient).send("0712345678", "Your OTP is 123456");
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void keepsTextItRouteWhenSelected() {
        ReflectionTestUtils.setField(service, "provider", "textit");
        ReflectionTestUtils.setField(service, "messageURI", "https://api.textit.biz/");
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1}"));

        assertTrue(service.sendMessage(MessageType.SENT_OTP_PASSWORD, "123456", null, "0712345678")
                .isSuccess());

        verify(hutchSmsClient, never()).send(anyString(), anyString());
    }

    @Test
    void sendsPlainTextForHutchEvenWhenTemplateContainsHtmlBreaks() {
        ReflectionTestUtils.setField(service, "provider", "hutch");
        NotificationTemplate template = new NotificationTemplate();
        template.setMessageBody("Claim {0} submitted.<br><br> Thank you!");
        when(templates.findByType(MessageType.SENT_OTP_PASSWORD)).thenReturn(Optional.of(template));
        when(hutchSmsClient.send("0712345678", "Claim 123456 submitted.\n\nThank you!"))
                .thenReturn(MessageResponseDTO.builder().success(true).build());

        assertTrue(service.sendMessage(MessageType.SENT_OTP_PASSWORD, "123456", null, "0712345678")
                .isSuccess());
        verify(hutchSmsClient).send("0712345678", "Claim 123456 submitted.\n\nThank you!");
    }

    @Test
    void sendsPlainTextForTextItEvenWhenTemplateContainsHtmlBreaks() {
        ReflectionTestUtils.setField(service, "provider", "textit");
        ReflectionTestUtils.setField(service, "messageURI", "https://api.textit.biz/");
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        NotificationTemplate template = new NotificationTemplate();
        template.setMessageBody("Claim {0} submitted.<br><br> Thank you!");
        when(templates.findByType(MessageType.SENT_OTP_PASSWORD)).thenReturn(Optional.of(template));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1}"));

        service.sendMessage(MessageType.SENT_OTP_PASSWORD, "123456", null, "0712345678");

        ArgumentCaptor<HttpEntity> sent = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), sent.capture(), eq(String.class));
        String body = (String) sent.getValue().getBody();
        assertTrue(body.contains("submitted.\\n\\nThank you!"));
        assertFalse(body.contains("<br"));
    }
}
