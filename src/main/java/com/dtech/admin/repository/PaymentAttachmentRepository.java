package com.dtech.admin.repository;

import com.dtech.admin.model.PaymentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentAttachmentRepository extends JpaRepository<PaymentAttachment, Long>, JpaSpecificationExecutor<PaymentAttachment> {

    Optional<PaymentAttachment> findTopByAttachmentPrefixAndAttachmentYearOrderByAttachmentSequenceDesc(String attachmentPrefix, Integer attachmentYear);
}
