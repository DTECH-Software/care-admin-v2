package com.dtech.admin.model;

import com.dtech.admin.enums.Workflow;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "employee_marital_status_update")
@ToString(exclude = "documents")
public class MaritalStatus extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false,updatable = false,unique = true)
    private Long id;

    @Column(name = "status",nullable = false)
    @Enumerated(EnumType.STRING)
    private Workflow status;

    @Column(name = "type",nullable = false)
    @Enumerated(EnumType.STRING)
    private com.dtech.admin.enums.MaritalStatus maritalStatus;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "marital_status_update_document",
            joinColumns = @JoinColumn(name = "employee_marital_status_update_id",referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "document_id",referencedColumnName = "id")
    )
    private List<Document> documents = new ArrayList<>();

    @JoinColumn(name = "user_id",nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private ApplicationUser applicationUser;
}
