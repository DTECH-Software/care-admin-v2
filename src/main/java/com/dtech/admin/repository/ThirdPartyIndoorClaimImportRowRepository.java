package com.dtech.admin.repository;

import com.dtech.admin.model.ThirdPartyIndoorClaimImportBatch;
import com.dtech.admin.model.ThirdPartyIndoorClaimImportRow;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.enums.ThirdPartyIndoorClaimRowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ThirdPartyIndoorClaimImportRowRepository extends JpaRepository<ThirdPartyIndoorClaimImportRow, Long> {
    List<ThirdPartyIndoorClaimImportRow> findAllByBatchOrderByRowNoAsc(ThirdPartyIndoorClaimImportBatch batch);
    boolean existsByExternalReferenceNoIgnoreCaseAndInsuranceClaimIsNotNull(String externalReferenceNo);
    boolean existsByInsuranceClaim_Id(Long insuranceClaimId);
    boolean existsByInsuranceClaim_IdAndStatus(Long insuranceClaimId, ThirdPartyIndoorClaimRowStatus status);
    List<ThirdPartyIndoorClaimImportRow> findAllByIntimatedDateBetweenAndInsuranceClaimIsNotNull(Date fromDate, Date toDate);
    List<ThirdPartyIndoorClaimImportRow> findAllByInsuranceClaimInAndStatus(List<InsuranceClaimsRequest> claims,
                                                                            ThirdPartyIndoorClaimRowStatus status);
    List<ThirdPartyIndoorClaimImportRow> findAllByStatusAndInsuranceClaimIsNotNull(ThirdPartyIndoorClaimRowStatus status);
    List<ThirdPartyIndoorClaimImportRow> findAllByStatusAndInsuranceClaimIsNotNullAndPaidDateBetween(
            ThirdPartyIndoorClaimRowStatus status, Date fromDate, Date toDate);
}

