package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.PaymentAdviceAttachmentSearchDTO;
import com.dtech.admin.enums.PaymentAttachmentStatus;
import com.dtech.admin.model.PaymentAdviceAttachment;
import com.dtech.admin.model.PaymentAttachment;
import com.dtech.admin.util.DateTimeUtil;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
public class PaymentAdviceAttachmentSpecification {

    private PaymentAdviceAttachmentSpecification() {
    }

    public static Specification<PaymentAttachment> getSpecification(PaymentAdviceAttachmentSearchDTO filterDto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<PaymentAdviceAttachment> subRoot = subquery.from(PaymentAdviceAttachment.class);
            subquery.select(subRoot.get("paymentAttachment").get("id"))
                    .where(cb.equal(subRoot.get("paymentAttachment").get("id"), root.get("id")));

            predicates.add(cb.not(cb.exists(subquery)));
            predicates.add(cb.equal(root.get("status"), PaymentAttachmentStatus.FINALIZED));

            if (hasText(filterDto.getAttachmentNo())) {
                predicates.add(cb.like(cb.lower(root.get("attachmentNo")),
                        "%" + filterDto.getAttachmentNo().toLowerCase() + "%"));
            }

            if (hasText(filterDto.getCompany())) {
                predicates.add(cb.equal(cb.lower(root.get("companyCode")), filterDto.getCompany().toLowerCase()));
            }

            List<String> staffCategoryCodes = filterDto.getStaffCategoryCodes();
            if (staffCategoryCodes != null && !staffCategoryCodes.isEmpty()) {
                staffCategoryCodes = staffCategoryCodes.stream()
                        .filter(PaymentAdviceAttachmentSpecification::hasText)
                        .map(String::toLowerCase)
                        .toList();
                predicates.add(cb.lower(root.get("staffCategoryCode")).in(staffCategoryCodes));
            } else if (hasText(filterDto.getStaffCategory())) {
                predicates.add(cb.equal(cb.lower(root.get("staffCategoryCode")), filterDto.getStaffCategory().toLowerCase()));
            }

            if (hasText(filterDto.getDateFrom())) {
                try {
                    Date fromDate = DateTimeUtil.getStartOfDay(normalizeDate(filterDto.getDateFrom()));
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), fromDate));
                } catch (Exception e) {
                    log.error("Failed to parse from date {}", filterDto.getDateFrom(), e);
                    throw new RuntimeException(e);
                }
            }

            if (hasText(filterDto.getDateTo())) {
                try {
                    Date toDate = DateTimeUtil.getEndOfDay(normalizeDate(filterDto.getDateTo()));
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), toDate));
                } catch (Exception e) {
                    log.error("Failed to parse to date {}", filterDto.getDateTo(), e);
                    throw new RuntimeException(e);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeDate(String value) {
        if (value.contains("-")) {
            return value.replace("-", "/");
        }
        return value;
    }
}
