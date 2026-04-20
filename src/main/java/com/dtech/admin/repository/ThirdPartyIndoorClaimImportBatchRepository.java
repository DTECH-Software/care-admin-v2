package com.dtech.admin.repository;

import com.dtech.admin.model.ThirdPartyIndoorClaimImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ThirdPartyIndoorClaimImportBatchRepository extends JpaRepository<ThirdPartyIndoorClaimImportBatch, Long>,
        JpaSpecificationExecutor<ThirdPartyIndoorClaimImportBatch> {
}

