package com.dtech.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class LinodeObjectStorageConfig {

    @Bean
    @ConditionalOnProperty(name = "wecare.object-storage.enabled", havingValue = "true")
    public S3Client linodeS3Client(
            @Value("${wecare.object-storage.endpoint}") String endpoint,
            @Value("${wecare.object-storage.signing-region:us-east-1}") String signingRegion,
            @Value("${wecare.object-storage.access-key}") String accessKey,
            @Value("${wecare.object-storage.secret-key}") String secretKey) {
        requireValue(endpoint, "LINODE_OBJECT_STORAGE_ENDPOINT");
        requireValue(signingRegion, "LINODE_OBJECT_STORAGE_SIGNING_REGION");
        requireValue(accessKey, "LINODE_OBJECT_STORAGE_ACCESS_KEY");
        requireValue(secretKey, "LINODE_OBJECT_STORAGE_SECRET_KEY");

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(signingRegion))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private void requireValue(String value, String environmentName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(environmentName + " is required when Object Storage is enabled");
        }
    }
}
