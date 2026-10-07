package com.dtech.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class HutchSmsClientTest {
    private static final String BASE_URL = "https://bsms.hutch.lk";

    @Test
    void logsInOnceAndReusesTokenForSubsequentSms() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        HutchSmsClient client = client(restTemplate);

        server.expect(requestTo(BASE_URL + "/api/login"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-VERSION", "v1"))
                .andExpect(content().json("{\"username\":\"user\",\"password\":\"secret\"}"))
                .andRespond(withSuccess("{\"accessToken\":\"token-one\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/sendsms"))
                .andExpect(header("Authorization", "Bearer token-one"))
                .andExpect(content().json("{\"campaignName\":\"WeCare\",\"mask\":\"WECARE\",\"numbers\":\"94712345678\",\"content\":\"First OTP\"}"))
                .andRespond(withSuccess("{\"serverRef\":32225}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/sendsms"))
                .andExpect(header("Authorization", "Bearer token-one"))
                .andExpect(content().json("{\"numbers\":\"94712345678\",\"content\":\"Second OTP\"}", false))
                .andRespond(withSuccess("{\"serverRef\":32226}", MediaType.APPLICATION_JSON));

        assertTrue(client.send("0712345678", "First OTP").isSuccess());
        assertTrue(client.send("+94712345678", "Second OTP").isSuccess());
        server.verify();
    }

    @Test
    void renewsTokenAndRetriesOnlyAfterUnauthorizedResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        HutchSmsClient client = client(restTemplate);

        server.expect(requestTo(BASE_URL + "/api/login"))
                .andRespond(withSuccess("{\"accessToken\":\"old-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/sendsms"))
                .andExpect(header("Authorization", "Bearer old-token"))
                .andRespond(withUnauthorizedRequest());
        server.expect(requestTo(BASE_URL + "/api/login"))
                .andRespond(withSuccess("{\"accessToken\":\"new-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/sendsms"))
                .andExpect(header("Authorization", "Bearer new-token"))
                .andRespond(withSuccess("{\"serverRef\":32227}", MediaType.APPLICATION_JSON));

        assertTrue(client.send("94712345678", "OTP").isSuccess());
        server.verify();
    }

    @Test
    void rejectsInvalidNumberWithoutCallingProvider() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        assertFalse(client(restTemplate).send("123", "OTP").isSuccess());
        server.verify();
    }

    @Test
    void normalizesSriLankanMobileNumbers() {
        assertEquals("94712345678", HutchSmsClient.normalizeMobile("0712345678"));
        assertEquals("94712345678", HutchSmsClient.normalizeMobile("712345678"));
        assertEquals("94712345678", HutchSmsClient.normalizeMobile("+94 71 234 5678"));
    }

    private HutchSmsClient client(RestTemplate restTemplate) {
        return new HutchSmsClient(restTemplate, BASE_URL, "user", "secret", "WECARE", "WeCare");
    }
}
