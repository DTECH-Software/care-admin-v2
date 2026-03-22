package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.PaymentAdviceSearchDTO;
import com.dtech.admin.enums.PaymentAdviceType;
import com.dtech.admin.model.PaymentAdvice;
import com.dtech.admin.util.DateTimeUtil;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
public class PaymentAdviceSpecification {

    private PaymentAdviceSpecification() {
    }

    public static Specification<PaymentAdvice> getSpecification(PaymentAdviceSearchDTO filterDto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.or(cb.isNull(root.get("type")),
                    cb.equal(root.get("type"), PaymentAdviceType.MEDICAL)));

            if (hasText(filterDto.getAdviceNo())) {
                String adviceNoFilter = "%" + filterDto.getAdviceNo().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("adviceNo")), adviceNoFilter),
                        cb.like(cb.lower(buildReturnChequeNoExpression(cb,
                                root.get("companyCode"),
                                root.get("staffCategoryCode"),
                                root.get("adviceYearStart").as(String.class),
                                root.get("adviceSequence"))), adviceNoFilter)
                ));
            }

            if (hasText(filterDto.getCompany())) {
                predicates.add(cb.equal(cb.lower(root.get("companyCode")), filterDto.getCompany().toLowerCase()));
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

    private static Expression<String> buildReturnChequeNoExpression(jakarta.persistence.criteria.CriteriaBuilder cb,
                                                                    Expression<String> companyCode,
                                                                    Expression<String> staffCategoryCode,
                                                                    Expression<String> adviceYearStart,
                                                                    Expression<Integer> adviceSequence) {
        Expression<String> paddedSequence = cb.<String>selectCase()
                .when(cb.lessThan(adviceSequence, 10), cb.concat("0", adviceSequence.as(String.class)))
                .otherwise(adviceSequence.as(String.class));

        return cb.concat(
                cb.concat(
                        cb.concat(
                                cb.concat(
                                        cb.concat(companyCode, " "),
                                        cb.concat(staffCategoryCode, " RETURN-")
                                ),
                                adviceYearStart
                        ),
                        "-"
                ),
                paddedSequence
        );
    }

    private static String normalizeDate(String value) {
        if (value.contains("-")) {
            return value.replace("-", "/");
        }
        return value;
    }
}
