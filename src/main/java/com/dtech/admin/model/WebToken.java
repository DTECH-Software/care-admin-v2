/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 8:10 AM
 * <p>
 */

package com.dtech.admin.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "web_token")
@Data
public class WebToken extends AdminAudit implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "access_token",nullable = false,updatable = false,unique = true)
    private String accessToken;

    @Column(name = "refresh_token",updatable = false,unique = true)
    private String refreshToken;

}
