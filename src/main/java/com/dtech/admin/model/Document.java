/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 2:28 PM
 * <p>
 */

package com.dtech.admin.model;


import com.dtech.admin.enums.DocType;
import com.dtech.admin.enums.DocumentStorageProvider;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "document")
@Data
@ToString(exclude = "doc")
public class Document extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "type",nullable = false)
    @Enumerated(EnumType.STRING)
    private DocType type;

    @Column(name = "doc")
    @Lob
    private String doc;

    @Column(name = "file_name",nullable = false)
    private String fileName;

    @Column(name = "file_type",nullable = false)
    private String fileType;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false)
    private DocumentStorageProvider storageProvider = DocumentStorageProvider.DATABASE;

    @Column(name = "bucket_name")
    private String bucketName;

    @Column(name = "object_key", unique = true, length = 500)
    private String objectKey;

    @Column(name = "object_size")
    private Long objectSize;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

}
