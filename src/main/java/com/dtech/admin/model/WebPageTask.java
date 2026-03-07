package com.dtech.admin.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "web_page_task")
@Data
@EqualsAndHashCode(exclude = {"webPage", "webTask"})
@ToString(exclude = {"webPage", "webTask"})
public class WebPageTask {

    @EmbeddedId
    private WebPageTaskId id = new WebPageTaskId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pageCode")
    @JoinColumn(name = "page_code", referencedColumnName = "code")
    private WebPage webPage;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("taskCode")
    @JoinColumn(name = "task_code", referencedColumnName = "code")
    private WebTask webTask;

}
