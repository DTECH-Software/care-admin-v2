package com.dtech.admin.repository;

import com.dtech.admin.model.PaymentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentAttachmentRepository extends JpaRepository<PaymentAttachment, Long>, JpaSpecificationExecutor<PaymentAttachment> {

    Optional<PaymentAttachment> findTopByAttachmentPrefixAndAttachmentYearOrderByAttachmentSequenceDesc(String attachmentPrefix, Integer attachmentYear);

    List<PaymentAttachment> findAllByCreatedDateBetween(Date fromDate, Date toDate);

    List<PaymentAttachment> findAllByLastModifiedDateBetween(Date fromDate, Date toDate);
}
