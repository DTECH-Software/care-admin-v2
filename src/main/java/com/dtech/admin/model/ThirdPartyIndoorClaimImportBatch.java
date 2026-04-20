package com.dtech.admin.model;

import com.dtech.admin.enums.ThirdPartyIndoorClaimBatchStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true, exclude = "rows")
@ToString(exclude = "rows")
@Entity
@Table(name = "third_party_indoor_claim_batch")
@Data
public class ThirdPartyIndoorClaimImportBatch extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "batch_no", nullable = false, unique = true)
    private String batchNo;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows;

    @Column(name = "valid_rows", nullable = false)
    private Integer validRows;

    @Column(name = "invalid_rows", nullable = false)
    private Integer invalidRows;

    @Column(name = "duplicate_rows", nullable = false)
    private Integer duplicateRows;

    @Column(name = "imported_rows", nullable = false)
    private Integer importedRows;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ThirdPartyIndoorClaimBatchStatus status;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ThirdPartyIndoorClaimImportRow> rows = new ArrayList<>();
}

