package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.PaymentAdviceDeathSearchDTO;
import com.dtech.admin.enums.PaymentAdviceType;
import com.dtech.admin.model.PaymentAdvice;
import com.dtech.admin.util.DateTimeUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
public class PaymentAdviceDeathSpecification {

    private PaymentAdviceDeathSpecification() {
    }

    public static Specification<PaymentAdvice> getSpecification(PaymentAdviceDeathSearchDTO filterDto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("type"), PaymentAdviceType.DEATH));

            if (hasText(filterDto.getAdviceNo())) {
                predicates.add(cb.like(cb.lower(root.get("adviceNo")), "%" + filterDto.getAdviceNo().toLowerCase() + "%"));
            }

            if (hasText(filterDto.getPaymentCompany())) {
                predicates.add(cb.equal(cb.lower(root.get("companyCode")), filterDto.getPaymentCompany().toLowerCase()));
            }

            if (hasText(filterDto.getStaffCategory())) {
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
