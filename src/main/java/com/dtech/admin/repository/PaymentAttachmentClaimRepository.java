package com.dtech.admin.repository;

import com.dtech.admin.enums.PaymentAttachmentClaimState;
import com.dtech.admin.enums.PaymentAttachmentStatus;
import com.dtech.admin.model.InsuranceClaimsRequest;
import com.dtech.admin.model.PaymentAttachment;
import com.dtech.admin.model.PaymentAttachmentClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAttachmentClaimRepository extends JpaRepository<PaymentAttachmentClaim, Long> {

    boolean existsByInsuranceClaimsRequestAndState(InsuranceClaimsRequest claim, PaymentAttachmentClaimState state);

    List<PaymentAttachmentClaim> findAllByPaymentAttachment(PaymentAttachment paymentAttachment);

    List<PaymentAttachmentClaim> findAllByInsuranceClaimsRequestIn(List<InsuranceClaimsRequest> claims);

    List<PaymentAttachmentClaim> findAllByInsuranceClaimsRequestInAndState(List<InsuranceClaimsRequest> claims,
                                                                           PaymentAttachmentClaimState state);

    boolean existsByInsuranceClaimsRequestAndPaymentAttachment_Status(InsuranceClaimsRequest claim,
                                                                      PaymentAttachmentStatus status);
}
