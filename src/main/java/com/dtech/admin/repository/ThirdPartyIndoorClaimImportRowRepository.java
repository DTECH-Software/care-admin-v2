package com.dtech.admin.repository;

import com.dtech.admin.model.ThirdPartyIndoorClaimImportBatch;
import com.dtech.admin.model.ThirdPartyIndoorClaimImportRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThirdPartyIndoorClaimImportRowRepository extends JpaRepository<ThirdPartyIndoorClaimImportRow, Long> {
    List<ThirdPartyIndoorClaimImportRow> findAllByBatchOrderByRowNoAsc(ThirdPartyIndoorClaimImportBatch batch);
    boolean existsByExternalReferenceNoIgnoreCaseAndInsuranceClaimIsNotNull(String externalReferenceNo);
}

