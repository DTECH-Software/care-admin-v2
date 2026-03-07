package com.dtech.admin.repository;

import com.dtech.admin.model.DocumentStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentStoreRepository extends JpaRepository<DocumentStore, Long> {
    Optional<DocumentStore> findByCode(com.dtech.admin.enums.DocumentStore code);
}
