/**
 * User: Himal_J
 * Date: 2/6/2025
 * Time: 8:33 AM
 * <p>
 */

package com.dtech.admin.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "web_password_policy")
@Data
public class WebPasswordPolicy extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "min_upper_case", nullable = false,length = 2)
    private int minUpperCase;

    @Column(name = "min_lower_case", nullable = false,length = 2)
    private int minLowerCase;

    @Column(name = "min_numbers", nullable = false,length = 2)
    private int minNumbers;

    @Column(name = "min_special_characters", nullable = false,length = 2)
    private int minSpecialCharacters;

    @Column(name = "min_length", nullable = false,length = 2)
    private int minLength;

    @Column(name = "max_length", nullable = false,length = 2)
    private int maxLength;

    @Column(name = "password_history",nullable = false,length = 2)
    private int passwordHistory;

    @Column(name = "attempt_exceed_count",nullable = false,length = 2)
    private int attemptExceedCount;

    @Column(name = "otp_exceed_count",nullable = false,length = 2)
    private int otpExceedCount;

}
