package com.dtech.admin.repository;

import com.dtech.admin.model.WebToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebTokenRepository extends JpaRepository<WebToken, Long> {
}
