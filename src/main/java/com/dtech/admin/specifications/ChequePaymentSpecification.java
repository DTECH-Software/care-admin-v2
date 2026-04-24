package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.ChequePaymentSearchDTO;
import com.dtech.admin.model.ChequePayment;
import com.dtech.admin.util.DateTimeUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
public class ChequePaymentSpecification {

    private ChequePaymentSpecification() {
    }

    public static Specification<ChequePayment> getSpecification(ChequePaymentSearchDTO searchDTO) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (hasText(searchDTO.getCompany())) {
                predicates.add(cb.equal(root.get("companyCode"), searchDTO.getCompany()));
            }

            List<String> staffCategoryCodes = searchDTO.getStaffCategoryCodes();
            if (staffCategoryCodes != null && !staffCategoryCodes.isEmpty()) {
                predicates.add(root.get("staffCategoryCode").in(staffCategoryCodes));
            } else if (hasText(searchDTO.getStaffCategory())) {
                predicates.add(cb.equal(root.get("staffCategoryCode"), searchDTO.getStaffCategory()));
            }

            if (hasText(searchDTO.getChequeNo())) {
                predicates.add(cb.like(cb.lower(root.get("chequeNo")),
                        "%" + searchDTO.getChequeNo().toLowerCase() + "%"));
            }

            if (hasText(searchDTO.getYear())) {
                predicates.add(cb.equal(root.get("year"), searchDTO.getYear()));
            }

            if (searchDTO.getMonths() != null && !searchDTO.getMonths().isEmpty()) {
                predicates.add(root.join("months").in(searchDTO.getMonths()));
            }

            if (hasText(searchDTO.getChequeDateFrom())) {
                try {
                    Date fromDate = DateTimeUtil.getStartOfDay(normalizeDate(searchDTO.getChequeDateFrom()));
                    predicates.add(cb.greaterThanOrEqualTo(root.get("chequeDate"), fromDate));
                } catch (Exception e) {
                    log.error("Failed to parse chequeDateFrom {}", searchDTO.getChequeDateFrom(), e);
                    throw new RuntimeException(e);
                }
            }

            if (hasText(searchDTO.getChequeDateTo())) {
                try {
                    Date toDate = DateTimeUtil.getEndOfDay(normalizeDate(searchDTO.getChequeDateTo()));
                    predicates.add(cb.lessThanOrEqualTo(root.get("chequeDate"), toDate));
                } catch (Exception e) {
                    log.error("Failed to parse chequeDateTo {}", searchDTO.getChequeDateTo(), e);
                    throw new RuntimeException(e);
                }
            }

            if (hasText(searchDTO.getAmountFrom())) {
                BigDecimal amountFrom = new BigDecimal(searchDTO.getAmountFrom());
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), amountFrom));
            }

            if (hasText(searchDTO.getAmountTo())) {
                BigDecimal amountTo = new BigDecimal(searchDTO.getAmountTo());
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), amountTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ChequePayment> getSpecification() {
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
