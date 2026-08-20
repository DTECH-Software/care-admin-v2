-- Upgrade source: docs/sql/admin-linode-object-storage.sql
-- Phase 1: WeCare Admin documents in Linode Object Storage.
-- Run before enabling WECARE_ADMIN_OBJECT_STORAGE_WRITE_ENABLED.
-- Keep WECARE_OBJECT_STORAGE_RETAIN_DATABASE_COPY=true during the Admin-only phase
ALTER TABLE document
    MODIFY COLUMN doc LONGTEXT NULL,
    ADD COLUMN storage_provider VARCHAR(30) NOT NULL DEFAULT 'DATABASE' AFTER file_type,
    ADD COLUMN bucket_name VARCHAR(255) NULL AFTER storage_provider,
    ADD COLUMN object_key VARCHAR(500) NULL AFTER bucket_name,
    ADD COLUMN object_size BIGINT NULL AFTER object_key,
    ADD COLUMN checksum_sha256 VARCHAR(64) NULL AFTER object_size;

CREATE UNIQUE INDEX uk_document_object_key ON document (object_key);
CREATE INDEX idx_document_storage_provider ON document (storage_provider);

UPDATE document
SET storage_provider = 'DATABASE'
WHERE storage_provider IS NULL OR storage_provider = '';
