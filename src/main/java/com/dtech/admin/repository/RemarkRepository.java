package com.dtech.admin.repository;

import com.dtech.admin.enums.RemarkCategory;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.Remark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RemarkRepository extends JpaRepository<Remark, Long> {
    List<Remark> findAllByRemarkCategoryAndStatus(RemarkCategory remarkCategory, Status status);
}
