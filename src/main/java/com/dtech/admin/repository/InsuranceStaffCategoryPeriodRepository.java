package com.dtech.admin.repository;

import com.dtech.admin.enums.Status;
import com.dtech.admin.model.InsuranceStaffCategoryPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceStaffCategoryPeriodRepository extends JpaRepository<InsuranceStaffCategoryPeriod, Long> {

    @Query("SELECT i FROM InsuranceStaffCategoryPeriod i WHERE :inputDate BETWEEN i.fromDate AND i.toDate and i.staffCategories.code = :staff ")
    Optional<InsuranceStaffCategoryPeriod> findByDateWithinRange(@Param("inputDate") Date inputDate,@Param("staff") String staff);

    @Query("SELECT i FROM InsuranceStaffCategoryPeriod i WHERE :inputDate >= i.fromDate AND :inputDate < i.toDate and i.staffCategories.code = :staff ")
    Optional<InsuranceStaffCategoryPeriod> findByDateWithinRangeExclusiveEnd(@Param("inputDate") Date inputDate,
                                                                              @Param("staff") String staff);

    @Query("SELECT i FROM InsuranceStaffCategoryPeriod i WHERE :inputDate BETWEEN i.fromDate AND i.toDate ORDER BY i.fromDate DESC")
    List<InsuranceStaffCategoryPeriod> findByDateWithinRangeAnyStaff(@Param("inputDate") Date inputDate);

    Optional<InsuranceStaffCategoryPeriod> findFirstByStaffCategories_CodeAndStatusAndFromDateAfterOrderByFromDateAsc(
            String staff, Status status, Date fromDate);

    List<InsuranceStaffCategoryPeriod> findByStaffCategories_CodeAndStatusAndFromDateGreaterThanEqualAndFromDateLessThanEqualOrderByFromDateAsc(
            String staff, Status status, Date startDate, Date endDate);

    Optional<InsuranceStaffCategoryPeriod> findFirstByToDateLessThanEqualOrderByToDateDesc(Date currentFromDate);

}
