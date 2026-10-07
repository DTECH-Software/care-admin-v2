package com.dtech.admin.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestConfig {
    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${message.connect-timeout-seconds:10}") long connectTimeoutSeconds,
            @Value("${message.read-timeout-seconds:15}") long readTimeoutSeconds) {
        return builder.connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .readTimeout(Duration.ofSeconds(readTimeoutSeconds)).build();
    }
}
