package com.dtech.admin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class WebPageTaskId implements Serializable {

    @Column(name = "page_code")
    private String pageCode;

    @Column(name = "task_code")
    private String taskCode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebPageTaskId)) return false;
        WebPageTaskId that = (WebPageTaskId) o;
        return Objects.equals(pageCode, that.pageCode) &&
               Objects.equals(taskCode, that.taskCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageCode, taskCode);
    }
}
