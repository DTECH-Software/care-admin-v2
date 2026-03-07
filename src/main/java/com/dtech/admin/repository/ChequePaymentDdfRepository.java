package com.dtech.admin.repository;

import com.dtech.admin.model.ChequePaymentDdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChequePaymentDdfRepository extends JpaRepository<ChequePaymentDdf, Long>, JpaSpecificationExecutor<ChequePaymentDdf> {
}
