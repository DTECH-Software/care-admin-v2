package com.dtech.admin.repository;

import com.dtech.admin.model.PaymentAdvice;
import com.dtech.admin.model.PaymentAdviceAttachment;
import com.dtech.admin.model.PaymentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface PaymentAdviceAttachmentRepository extends JpaRepository<PaymentAdviceAttachment, Long> {
    boolean existsByPaymentAttachment(PaymentAttachment paymentAttachment);

    List<PaymentAdviceAttachment> findAllByPaymentAttachmentIn(List<PaymentAttachment> paymentAttachments);

    List<PaymentAdviceAttachment> findAllByPaymentAdvice(PaymentAdvice paymentAdvice);

    List<PaymentAdviceAttachment> findAllByCreatedDateBetween(Date fromDate, Date toDate);
}
