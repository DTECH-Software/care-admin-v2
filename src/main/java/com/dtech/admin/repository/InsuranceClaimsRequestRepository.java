package com.dtech.admin.repository;


import com.dtech.admin.enums.Workflow;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.InsuranceClaimsRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Repository
public interface InsuranceClaimsRequestRepository extends JpaRepository<InsuranceClaimsRequest, Long>, JpaSpecificationExecutor<InsuranceClaimsRequest> {

    @Query("SELECT SUM(ic.requestAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "LEFT OUTER JOIN InsuranceDetailsLimit idl ON idl.id = ic.insuranceDetailsLimit.id " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod ip ON idl.insuranceStaffCategoryPeriod.id = ip.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ip.id = :insurancePeriod " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumRequestAmountByEmployeeAndTreatmentAndStatus(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("insurancePeriod") Long insurancePeriod,
            @Param("statuses") List<Workflow> statuses);



    @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumRequestAmountByEmployeeAndTreatmentAndStatus(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("statuses") List<Workflow> statuses);

    @Query("SELECT SUM(COALESCE(ic.approvedAmount, ic.requestAmount)) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumApprovedAmountByEmployeeAndTreatment(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("statuses") List<Workflow> statuses);

    @Query("SELECT SUM(ic.requestAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "LEFT OUTER JOIN InsuranceDetailsLimit idl ON idl.id = ic.insuranceDetailsLimit.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND idl.id = :insuranceDetailsLimit " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumRequestAmountByEmployee(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("insuranceDetailsLimit") Long insuranceDetailsLimit,
            @Param("statuses") List<Workflow> statuses);


    @Query("SELECT SUM(ic.requestAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumRequestAmountByEmployee(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("statuses") List<Workflow> statuses);

    @Query("SELECT SUM(ic.requestAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "LEFT OUTER JOIN InsuranceDetailsLimit idl ON idl.id = ic.insuranceDetailsLimit.id " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod ip ON idl.insuranceStaffCategoryPeriod.id = ip.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND icd.treatmentCategory.code = :treatmentCategory " +
            "AND ip.id = :insurancePeriod " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("treatmentCategory") String treatmentCategory,
            @Param("insurancePeriod") Long insurancePeriod,
            @Param("statuses") List<Workflow> statuses);

    @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "LEFT OUTER JOIN InsuranceDetailsLimit idl ON idl.id = ic.insuranceDetailsLimit.id " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod ip ON idl.insuranceStaffCategoryPeriod.id = ip.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND icd.treatmentCategory.code = :treatmentCategory " +
            "AND ip.id = :insurancePeriod " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumApprovedAmountByEmployeeAndTreatmentAndTreatmentCategoryAndPeriod(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("treatmentCategory") String treatmentCategory,
            @Param("insurancePeriod") Long insurancePeriod,
            @Param("statuses") List<Workflow> statuses);

    @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "LEFT OUTER JOIN InsuranceDetailsLimit idl ON idl.id = ic.insuranceDetailsLimit.id " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod ip ON idl.insuranceStaffCategoryPeriod.id = ip.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ip.id = :insurancePeriod " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumApprovedAmountByEmployeeAndTreatmentAndPeriod(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("insurancePeriod") Long insurancePeriod,
            @Param("statuses") List<Workflow> statuses);

    @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "LEFT OUTER JOIN InsuranceDetailsLimit idl ON idl.id = ic.insuranceDetailsLimit.id " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod ip ON idl.insuranceStaffCategoryPeriod.id = ip.id " +
            "LEFT OUTER JOIN InsuranceQuarter iq ON ic.insuranceQuarter.id = iq.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ip.id = :insurancePeriod " +
            "AND (" +
            "     (iq.fromDate = :fromDate AND iq.toDate = :toDate) " +
            "     OR (ic.insuranceQuarter IS NULL)" +
            ") " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumApprovedAmountByEmployeeAndTreatmentAndPeriodAndQuarterRange(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("insurancePeriod") Long insurancePeriod,
            @Param("fromDate") java.util.Date fromDate,
            @Param("toDate") java.util.Date toDate,
            @Param("statuses") List<Workflow> statuses);

    @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ic.createdDate >= :fromDate " +
            "AND ic.createdDate <= :toDate " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumApprovedAmountByEmployeeAndTreatmentBetweenDates(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("fromDate") java.util.Date fromDate,
            @Param("toDate") java.util.Date toDate,
            @Param("statuses") List<Workflow> statuses);

    @Query("SELECT ip FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "LEFT OUTER JOIN InsuranceDetailsLimit idl ON idl.id = ic.insuranceDetailsLimit.id " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod ip ON idl.insuranceStaffCategoryPeriod.id = ip.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ip.fromDate < :currentFromDate " +
            "ORDER BY ip.fromDate DESC")
    List<com.dtech.admin.model.InsuranceStaffCategoryPeriod> findPreviousPeriodsForEmployeeAndTreatment(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("currentFromDate") java.util.Date currentFromDate,
            Pageable pageable);

    @Query("SELECT ip FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceDetailsLimit idl ON idl.id = ic.insuranceDetailsLimit.id " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod ip ON idl.insuranceStaffCategoryPeriod.id = ip.id " +
            "WHERE ic.employee = :employee " +
            "AND ip.toDate < :currentFromDate " +
            "ORDER BY ip.toDate DESC")
    List<com.dtech.admin.model.InsuranceStaffCategoryPeriod> findPreviousPeriodsForEmployee(
            @Param("employee") ApplicationUser employee,
            @Param("currentFromDate") java.util.Date currentFromDate,
            Pageable pageable);

    @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND icd.treatmentCategory.code = :treatmentCategory " +
            "AND ic.createdDate >= :fromDate " +
            "AND ic.createdDate <= :toDate " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumApprovedAmountByEmployeeAndTreatmentAndCategoryBetweenDates(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("treatmentCategory") String treatmentCategory,
            @Param("fromDate") java.util.Date fromDate,
            @Param("toDate") java.util.Date toDate,
            @Param("statuses") List<Workflow> statuses);

    @Query("SELECT SUM(ic.approvedAmount) FROM InsuranceClaimsRequest ic " +
            "LEFT OUTER JOIN InsuranceClaimsDetails icd ON ic.insuranceClaimsDetails.id = icd.id " +
            "LEFT OUTER JOIN InsuranceDetailsLimit idl ON idl.id = ic.insuranceDetailsLimit.id " +
            "LEFT OUTER JOIN InsuranceStaffCategoryPeriod ip ON idl.insuranceStaffCategoryPeriod.id = ip.id " +
            "WHERE ic.employee = :employee " +
            "AND icd.treatment.treatmentCode = :treatment " +
            "AND ip.staffCategories.code = :staffCategory " +
            "AND ic.createdDate >= :fromDate " +
            "AND ic.createdDate <= :toDate " +
            "AND ic.requestStatus IN :statuses")
    BigDecimal getSumApprovedAmountByEmployeeAndTreatmentAndStaffCategoryBetweenDates(
            @Param("employee") ApplicationUser employee,
            @Param("treatment") String treatment,
            @Param("staffCategory") String staffCategory,
            @Param("fromDate") java.util.Date fromDate,
            @Param("toDate") java.util.Date toDate,
            @Param("statuses") List<Workflow> statuses);

    int countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndRequestStatusInAndEmployee(String treatmentCode, List<Workflow> workflow, ApplicationUser applicationUser);

    int countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndRequestStatusIn(String treatmentCode, List<Workflow> workflow);

    boolean existsByEmployeeAndInsuranceClaimsDetails_Treatment_TreatmentCodeAndRequestStatus(ApplicationUser employee, String code, Workflow requestStatus);
    int countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndInsuranceClaimsDetails_InsuranceStaffCategoryPeriod_IdAndInsuranceDetailsLimit_InsurancePolicy_IdAndRequestStatusInAndEmployee(
            String treatmentCode, Long insuranceStaffCategoryPeriodId, Long insurancePolicyId, List<Workflow> workflow, ApplicationUser applicationUser);
    int countByInsuranceClaimsDetails_Treatment_TreatmentCodeAndInsuranceClaimsDetails_InsuranceStaffCategoryPeriod_IdAndInsuranceDetailsLimit_InsurancePolicy_IdAndRequestStatusIn(
            String treatmentCode, Long insuranceStaffCategoryPeriodId, Long insurancePolicyId, List<Workflow> workflow);
    boolean existsByEmployeeAndInsuranceClaimsDetails_Treatment_TreatmentCodeAndInsuranceClaimsDetails_InsuranceStaffCategoryPeriod_IdAndInsuranceDetailsLimit_InsurancePolicy_IdAndRequestStatus(
            ApplicationUser employee, String code, Long insuranceStaffCategoryPeriodId, Long insurancePolicyId, Workflow requestStatus);

    long countByRequestStatus(Workflow requestStatus);

    List<InsuranceClaimsRequest> findAllByCreatedDateBetween(Date fromDate, Date toDate);

    List<InsuranceClaimsRequest> findAllByEmployeeAndRequestStatusIn(ApplicationUser employee, List<Workflow> statuses);
}
