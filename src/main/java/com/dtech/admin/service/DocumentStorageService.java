package com.dtech.admin.service;

import com.dtech.admin.enums.DocumentStorageProvider;
import com.dtech.admin.model.Document;
import com.dtech.admin.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class DocumentStorageService {
    private final DocumentRepository documentRepository;
    private final ObjectProvider<S3Client> s3ClientProvider;

    @Value("${wecare.object-storage.admin-write-enabled:false}")
    private boolean adminWriteEnabled;

    @Value("${wecare.object-storage.bucket:}")
    private String configuredBucket;

    @Value("${wecare.object-storage.retain-database-copy:true}")
    private boolean retainDatabaseCopy;

    @Value("${wecare.object-storage.admin-prefix:admin}")
    private String adminPrefix;

    @Transactional
    public Document saveAdminDocument(Document document, String base64Content) {
        if (!adminWriteEnabled) {
            document.setDoc(base64Content);
            document.setStorageProvider(DocumentStorageProvider.DATABASE);
            clearObjectMetadata(document);
            return documentRepository.saveAndFlush(document);
        }

        byte[] bytes = decodeBase64(base64Content);
        String bucket = requireBucket(document.getBucketName());
        String objectKey = buildObjectKey(document);
        String checksum = checksum(bytes);
        S3Client s3Client = requireClient();

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(document.getFileType())
                .contentLength((long) bytes.length)
                .metadata(Map.of("sha256", checksum, "source", "wecare-admin"))
                .build();
        s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));

        // Phase 1 keeps a compatibility copy for Care-App services that still read
        // document.doc directly. Disable only after Care-App supports object reads.
        document.setDoc(retainDatabaseCopy ? base64Content : null);
        document.setStorageProvider(DocumentStorageProvider.LINODE_OBJECT_STORAGE);
        document.setBucketName(bucket);
        document.setObjectKey(objectKey);
        document.setObjectSize((long) bytes.length);
        document.setChecksumSha256(checksum);

        try {
            Document savedDocument = documentRepository.saveAndFlush(document);
            deleteObjectIfTransactionRollsBack(s3Client, bucket, objectKey);
            return savedDocument;
        } catch (RuntimeException databaseFailure) {
            deleteQuietly(s3Client, bucket, objectKey);
            throw databaseFailure;
        }
    }

    @Transactional(readOnly = true)
    public String getBase64(Document document) {
        if (document == null) return null;
        if (document.getStorageProvider() != DocumentStorageProvider.LINODE_OBJECT_STORAGE
                || !StringUtils.hasText(document.getObjectKey())) {
            return document.getDoc();
        }

        // During the compatibility phase this preserves the exact legacy response,
        // including any formatting used by existing clients.
        if (StringUtils.hasText(document.getDoc())) return document.getDoc();

        String bucket = requireBucket(document.getBucketName());
        ResponseBytes<GetObjectResponse> object = requireClient().getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(document.getObjectKey()).build());
        byte[] bytes = object.asByteArray();
        if (StringUtils.hasText(document.getChecksumSha256())
                && !document.getChecksumSha256().equalsIgnoreCase(checksum(bytes))) {
            throw new IllegalStateException("Document checksum validation failed for document id=" + document.getId());
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    private byte[] decodeBase64(String content) {
        if (!StringUtils.hasText(content)) throw new IllegalArgumentException("Document content is required");
        String normalized = content.trim();
        int separator = normalized.indexOf(',');
        if (normalized.startsWith("data:") && separator >= 0) normalized = normalized.substring(separator + 1);
        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException invalidBase64) {
            log.warn("Document content was not Base64 encoded; preserving the original byte representation");
            return normalized.getBytes(StandardCharsets.UTF_8);
        }
    }

    private String buildObjectKey(Document document) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String type = document.getType() == null ? "document" : document.getType().name().toLowerCase();
        return cleanPrefix(adminPrefix) + "/" + type + "/" + today.getYear() + "/"
                + String.format("%02d", today.getMonthValue()) + "/" + UUID.randomUUID() + extension(document.getFileName());
    }

    private String cleanPrefix(String value) {
        String cleaned = value == null ? "admin" : value.replaceAll("^/+|/+$", "");
        return cleaned.isBlank() ? "admin" : cleaned;
    }

    private String extension(String fileName) {
        if (!StringUtils.hasText(fileName)) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        String extension = fileName.substring(dot + 1).replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return extension.isBlank() || extension.length() > 10 ? "" : "." + extension;
    }

    private String checksum(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to calculate document checksum", e);
        }
    }

    private S3Client requireClient() {
        S3Client client = s3ClientProvider.getIfAvailable();
        if (client == null) throw new IllegalStateException("Linode Object Storage is not enabled or configured");
        return client;
    }

    private String requireBucket(String documentBucket) {
        String bucket = StringUtils.hasText(documentBucket) ? documentBucket : configuredBucket;
        if (!StringUtils.hasText(bucket)) throw new IllegalStateException("LINODE_OBJECT_STORAGE_BUCKET is required");
        return bucket;
    }

    private void clearObjectMetadata(Document document) {
        document.setBucketName(null);
        document.setObjectKey(null);
        document.setObjectSize(null);
        document.setChecksumSha256(null);
    }

    private void deleteQuietly(S3Client client, String bucket, String objectKey) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
        } catch (Exception cleanupFailure) {
            log.error("Unable to remove orphaned Object Storage item key={}", objectKey, cleanupFailure);
        }
    }

    private void deleteObjectIfTransactionRollsBack(S3Client client, String bucket, String objectKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteQuietly(client, bucket, objectKey);
                }
            }
        });
    }
}
