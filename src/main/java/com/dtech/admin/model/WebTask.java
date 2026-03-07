/**
 * User: Himal_J
 * Date: 4/22/2025
 * Time: 8:09 AM
 * <p>
 */

package com.dtech.admin.model;

import com.dtech.admin.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true, exclude = {"webPageTasks"})
@ToString(exclude = {"webPageTasks"})
@Entity
@Table(name = "web_task")
@Data
public class WebTask extends AdminAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "code",nullable = false,updatable = false,   unique = true)
    private String code;

    @Column(name = "description",nullable = false)
    private String description;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

//    @ManyToMany(mappedBy = "webPageTasks")
//    private List<WebPage> webPages = new ArrayList<>();

    @OneToMany(mappedBy = "webTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WebPageTask> webPageTasks = new ArrayList<>();

}
