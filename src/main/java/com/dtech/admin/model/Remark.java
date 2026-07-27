package com.dtech.admin.model;

import com.dtech.admin.enums.RemarkCategory;
import com.dtech.admin.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "remark")
@Data
public class Remark extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "remark_category",nullable = false)
    @Enumerated(EnumType.STRING)
    private RemarkCategory remarkCategory;

    @Column(name = "code",nullable = false)
    private String code;

    @Column(name = "description",nullable = false)
    private String description;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "include_in_rejected_claim_report", nullable = false)
    private boolean includeInRejectedClaimReport = true;

}
