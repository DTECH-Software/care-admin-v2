package com.dtech.admin.repository;

import com.dtech.admin.enums.PaymentAdviceType;
import com.dtech.admin.model.PaymentAdvice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PaymentAdviceRepository extends JpaRepository<PaymentAdvice, Long>, JpaSpecificationExecutor<PaymentAdvice> {
    Optional<PaymentAdvice> findTopByAdviceYearStartAndStaffCategoryCodeOrderByAdviceSequenceDesc(Integer adviceYearStart, String staffCategoryCode);

    Optional<PaymentAdvice> findTopByOrderByVoucherSequenceDesc();

    @Query("select p from PaymentAdvice p where p.adviceYearStart = :yearStart and p.staffCategoryCode = :staffCategoryCode and (p.type = :type or p.type is null) order by p.adviceSequence desc")
    List<PaymentAdvice> findByAdviceYearStartAndStaffCategoryCodeAndTypeOrNullOrderByAdviceSequenceDesc(
            @Param("yearStart") Integer yearStart,
            @Param("staffCategoryCode") String staffCategoryCode,
            @Param("type") PaymentAdviceType type,
            Pageable pageable);

    Optional<PaymentAdvice> findTopByAdviceYearStartAndStaffCategoryCodeAndTypeOrderByAdviceSequenceDesc(
            Integer adviceYearStart, String staffCategoryCode, PaymentAdviceType type);

    @Query("select p from PaymentAdvice p where (p.type = :type or p.type is null) order by p.voucherSequence desc")
    List<PaymentAdvice> findByTypeOrNullOrderByVoucherSequenceDesc(@Param("type") PaymentAdviceType type, Pageable pageable);

    Optional<PaymentAdvice> findTopByTypeOrderByVoucherSequenceDesc(PaymentAdviceType type);

    Optional<PaymentAdvice> findTopByTypeOrderByAdviceSequenceDesc(PaymentAdviceType type);

    List<PaymentAdvice> findAllByCreatedDateBetweenAndType(Date fromDate, Date toDate, PaymentAdviceType type);
}
