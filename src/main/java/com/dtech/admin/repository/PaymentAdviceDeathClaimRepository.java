package com.dtech.admin.repository;

import com.dtech.admin.model.DeathClaimRequest;
import com.dtech.admin.model.PaymentAdvice;
import com.dtech.admin.model.PaymentAdviceDeathClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface PaymentAdviceDeathClaimRepository extends JpaRepository<PaymentAdviceDeathClaim, Long> {
    boolean existsByDeathClaim(DeathClaimRequest deathClaim);

    List<PaymentAdviceDeathClaim> findAllByPaymentAdvice(PaymentAdvice paymentAdvice);

    List<PaymentAdviceDeathClaim> findAllByDeathClaimIn(List<DeathClaimRequest> deathClaims);

    List<PaymentAdviceDeathClaim> findAllByCreatedDateBetween(Date fromDate, Date toDate);
}
