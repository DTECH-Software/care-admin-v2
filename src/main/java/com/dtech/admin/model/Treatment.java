/**
 * User: Himal_J
 * Date: 3/1/2025
 * Time: 9:32 PM
 * <p>
 */

package com.dtech.admin.model;

import com.dtech.admin.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "treatment")
@Data
public class Treatment extends AdminAudit implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "code",nullable = false,updatable = false,unique = true)
    private String treatmentCode;

    @Column(name = "description",nullable = false)
    private String treatmentDescription;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

}
