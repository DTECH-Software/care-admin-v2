package com.dtech.admin.repository;

import com.dtech.admin.model.ChequePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ChequePaymentRepository extends JpaRepository<ChequePayment, Long>, JpaSpecificationExecutor<ChequePayment> {
}
