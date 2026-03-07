
/**
 * User: Himal_J
 * Date: 4/22/2025
 * Time: 8:07 AM
 * <p>
 */
package com.dtech.admin.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Entity
@Table(name = "web_user_role_page_task")
@Data
public class WebUserRolePageTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "role_code", referencedColumnName = "code")
    @JsonIgnore
    private WebUserRole role;

    @ManyToOne
    @JoinColumn(name = "page_code", referencedColumnName = "code")
    @JsonIgnore
    private WebPage page;

    @ManyToOne
    @JoinColumn(name = "task_code", referencedColumnName = "code")
    @JsonIgnore
    private WebTask task;
}
