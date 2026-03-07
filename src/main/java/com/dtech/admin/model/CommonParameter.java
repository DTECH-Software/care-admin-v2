/**
 * User: Himal_J
 * Date: 3/5/2025
 * Time: 12:43 PM
 * <p>
 */

package com.dtech.admin.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "common_paramter")
@Data
public class CommonParameter extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "code",nullable = false,updatable = false,   unique = true)
    private String code;

    @Column(name = "description",nullable = false)
    private String description;

    @Column(name = "value",nullable = false)
    @Size(min = 10, max = 365)
    private int value;

}
