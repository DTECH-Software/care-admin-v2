package com.dtech.admin.repository;

import com.dtech.admin.enums.RemarkCategory;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.Remark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RemarkRepository extends JpaRepository<Remark, Long> {
    List<Remark> findAllByRemarkCategoryAndStatus(RemarkCategory remarkCategory, Status status);
    Optional<Remark> findFirstByCodeIgnoreCaseAndRemarkCategoryAndStatus(String code, RemarkCategory remarkCategory, Status status);
}
