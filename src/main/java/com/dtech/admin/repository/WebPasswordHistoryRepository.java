package com.dtech.admin.repository;

import com.dtech.admin.model.WebPasswordHistory;
import com.dtech.admin.model.WebUser;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebPasswordHistoryRepository extends JpaRepository<WebPasswordHistory, Long> {
    List<WebPasswordHistory> findByWebUserAndPassword(WebUser webUser, String password, Sort sort);
}
