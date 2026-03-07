/**
 * User: Himal_J
 * Date: 4/22/2025
 * Time: 8:07 AM
 * <p>
 */

package com.dtech.admin.model;

import com.dtech.admin.enums.Status;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true, exclude = {"webPageTasks", "webSection"})
@ToString(exclude = {"webPageTasks", "webSection"})
@Entity
@Table(name = "web_page")
@Data
@JsonIgnoreProperties({"webPageTasks", "webSection"})
public class WebPage extends AdminAudit implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "code",nullable = false,updatable = false,   unique = true)
    private String code;

    @Column(name = "url",nullable = false,updatable = false,   unique = true)
    private String url;

    @Column(name = "description",nullable = false)
    private String description;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section",referencedColumnName = "code",nullable = false)
    private WebSection webSection;

//    @ManyToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
//    @JoinTable(
//            name = "web_page_task",
//            joinColumns = @JoinColumn(name = "page_code",referencedColumnName = "code"),
//            inverseJoinColumns = @JoinColumn(name = "task_code",referencedColumnName = "code")
//    )
//    @JsonIgnore
//    private List<WebTask> webPageTasks = new ArrayList<>();

    @OneToMany(mappedBy = "webPage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WebPageTask> webPageTasks = new ArrayList<>();
}
