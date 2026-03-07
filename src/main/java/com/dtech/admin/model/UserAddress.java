/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 9:21 AM
 * <p>
 */

package com.dtech.admin.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_address")
@Data
public class UserAddress extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "street_no",nullable = false)
    private String streetNo;

    @Column(name = "street_1",nullable = false)
    private String street1;

    @Column(name = "street_2")
    private String street2;

    @Column(name = "city",nullable = false)
    private String city;

    @OneToOne(mappedBy = "userAddress")
    private UserPersonalDetails userPersonalDetails;

}
