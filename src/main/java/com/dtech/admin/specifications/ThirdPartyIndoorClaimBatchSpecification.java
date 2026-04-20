package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.ThirdPartyIndoorClaimBatchSearchDTO;
import com.dtech.admin.enums.ThirdPartyIndoorClaimBatchStatus;
import com.dtech.admin.model.ThirdPartyIndoorClaimImportBatch;
import com.dtech.admin.util.DateTimeUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
public class ThirdPartyIndoorClaimBatchSpecification {

    private ThirdPartyIndoorClaimBatchSpecification() {
    }

    public static Specification<ThirdPartyIndoorClaimImportBatch> getSpecification(ThirdPartyIndoorClaimBatchSearchDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filter.getBatchNo())) {
                predicates.add(cb.like(cb.lower(root.get("batchNo")), "%" + filter.getBatchNo().toLowerCase() + "%"));
            }

            if (hasText(filter.getFileName())) {
                predicates.add(cb.like(cb.lower(root.get("fileName")), "%" + filter.getFileName().toLowerCase() + "%"));
            }

            if (hasText(filter.getUploadedBy())) {
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + filter.getUploadedBy().toLowerCase() + "%"));
            }

            if (hasText(filter.getStatus())) {
                predicates.add(cb.equal(root.get("status"), ThirdPartyIndoorClaimBatchStatus.valueOf(filter.getStatus().trim().toUpperCase())));
            }

            if (hasText(filter.getFromDate())) {
                try {
                    Date fromDate = DateTimeUtil.getStartOfDay(normalizeDate(filter.getFromDate()));
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), fromDate));
                } catch (Exception e) {
                    log.error("Failed to parse from date {}", filter.getFromDate(), e);
                    throw new RuntimeException(e);
                }
            }

            if (hasText(filter.getToDate())) {
                try {
                    Date toDate = DateTimeUtil.getEndOfDay(normalizeDate(filter.getToDate()));
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), toDate));
                } catch (Exception e) {
                    log.error("Failed to parse to date {}", filter.getToDate(), e);
                    throw new RuntimeException(e);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ThirdPartyIndoorClaimImportBatch> getSpecification() {
        return (root, query, cb) -> cb.conjunction();
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

