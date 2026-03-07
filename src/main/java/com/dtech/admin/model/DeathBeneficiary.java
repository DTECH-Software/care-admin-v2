/**
 * User: Himal_J
 * Date: 3/16/2025
 * Time: 8:14 AM
 * <p>
 */

package com.dtech.admin.model;

import com.dtech.admin.enums.Range;
import com.dtech.admin.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "death_beneficiary")
@Data
public class DeathBeneficiary extends Audit implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "code",updatable = false,unique = true)
    @Enumerated(EnumType.STRING)
    private com.dtech.admin.enums.DeathBeneficiary code;

    @Column(name = "claim_limit",nullable = false)
    private BigDecimal claimLimit;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "range")
    @Enumerated(EnumType.STRING)
    private Range range;

}
