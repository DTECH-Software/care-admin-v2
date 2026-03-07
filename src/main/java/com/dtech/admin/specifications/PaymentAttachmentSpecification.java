package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.PaymentAttachmentSearchDTO;
import com.dtech.admin.enums.PaymentAttachmentStatus;
import com.dtech.admin.model.PaymentAttachment;
import com.dtech.admin.util.DateTimeUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Log4j2
public class PaymentAttachmentSpecification {

    private PaymentAttachmentSpecification() {
    }

    public static Specification<PaymentAttachment> getSpecification(PaymentAttachmentSearchDTO filterDto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filterDto.getAttachmentNo())) {
                predicates.add(cb.like(cb.lower(root.get("attachmentNo")), "%" + filterDto.getAttachmentNo().toLowerCase() + "%"));
            }

            if (hasText(filterDto.getCompany())) {
                predicates.add(cb.equal(cb.lower(root.get("companyCode")), filterDto.getCompany().toLowerCase()));
            }

            if (hasText(filterDto.getStaffCategory())) {
                predicates.add(cb.equal(cb.lower(root.get("staffCategoryCode")), filterDto.getStaffCategory().toLowerCase()));
            }

            if (hasText(filterDto.getTreatmentCategory())) {
                predicates.add(cb.equal(cb.lower(root.get("treatmentCategory")), filterDto.getTreatmentCategory().toLowerCase()));
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

            List<PaymentAttachmentStatus> statuses = resolveStatuses(filterDto.getStatus());
            if (!statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static List<PaymentAttachmentStatus> resolveStatuses(List<String> statuses) {
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return List.of();
        }
        return statuses.stream()
                .filter(PaymentAttachmentSpecification::hasText)
                .map(String::toUpperCase)
                .map(PaymentAttachmentStatus::valueOf)
                .collect(Collectors.toList());
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
