-- Upgrade source: docs/sql/admin-linode-object-storage.sql
-- Phase 1: WeCare Admin documents in Linode Object Storage.
-- Run before enabling WECARE_ADMIN_OBJECT_STORAGE_WRITE_ENABLED.
-- Keep WECARE_OBJECT_STORAGE_RETAIN_DATABASE_COPY=true during the Admin-only phase
ALTER TABLE document
    MODIFY COLUMN type VARCHAR(50) NOT NULL,
    MODIFY COLUMN doc LONGTEXT NULL;

SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'document' AND column_name = 'storage_provider'), 'SELECT 1', 'ALTER TABLE document ADD COLUMN storage_provider VARCHAR(30) NOT NULL DEFAULT ''DATABASE'' AFTER file_type');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'document' AND column_name = 'bucket_name'), 'SELECT 1', 'ALTER TABLE document ADD COLUMN bucket_name VARCHAR(255) NULL AFTER storage_provider');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'document' AND column_name = 'object_key'), 'SELECT 1', 'ALTER TABLE document ADD COLUMN object_key VARCHAR(500) NULL AFTER bucket_name');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'document' AND column_name = 'object_size'), 'SELECT 1', 'ALTER TABLE document ADD COLUMN object_size BIGINT NULL AFTER object_key');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'document' AND column_name = 'checksum_sha256'), 'SELECT 1', 'ALTER TABLE document ADD COLUMN checksum_sha256 VARCHAR(64) NULL AFTER object_size');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'document' AND index_name = 'uk_document_object_key'), 'SELECT 1', 'CREATE UNIQUE INDEX uk_document_object_key ON document (object_key)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'document' AND index_name = 'idx_document_storage_provider'), 'SELECT 1', 'CREATE INDEX idx_document_storage_provider ON document (storage_provider)');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE document
SET storage_provider = 'DATABASE'
WHERE storage_provider IS NULL OR storage_provider = '';

ALTER TABLE document
    MODIFY COLUMN storage_provider VARCHAR(30) NOT NULL DEFAULT 'DATABASE';
