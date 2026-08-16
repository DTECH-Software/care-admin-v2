package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.SupportTicketSearchDTO;
import com.dtech.admin.enums.SupportTicketSystemType;
import com.dtech.admin.model.SupportTicket;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public final class SupportTicketSpecification {
    private SupportTicketSpecification() { }

    public static Specification<SupportTicket> filter(SupportTicketSystemType systemType,
                                                       Collection<String> companyCodes,
                                                       SupportTicketSearchDTO search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("systemType"), systemType));
            predicates.add(root.join("company").get("code").in(companyCodes));
            if (search == null) return cb.and(predicates.toArray(new Predicate[0]));
            if (hasText(search.getTicketNo()))
                predicates.add(cb.like(cb.lower(root.get("ticketNo")), contains(search.getTicketNo())));
            if (hasText(search.getCompanyCode()))
                predicates.add(cb.equal(cb.lower(root.join("company").get("code")), search.getCompanyCode().trim().toLowerCase()));
            if (hasText(search.getCategory()))
                predicates.add(cb.equal(cb.lower(root.get("category")), search.getCategory().trim().toLowerCase()));
            if (hasText(search.getSubject()))
                predicates.add(cb.like(cb.lower(root.get("subject")), contains(search.getSubject())));
            if (hasText(search.getPriority()))
                predicates.add(cb.equal(root.get("priority").as(String.class), search.getPriority().trim().toUpperCase()));
            if (hasText(search.getStatus()))
                predicates.add(cb.equal(root.get("status").as(String.class), search.getStatus().trim().toUpperCase()));
            if (hasText(search.getCreatedBy()))
                predicates.add(cb.like(cb.lower(root.get("createdBy")), contains(search.getCreatedBy())));
            if (search.getFromDate() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), search.getFromDate()));
            if (search.getToDate() != null)
                predicates.add(cb.lessThan(root.get("createdDate"), nextDay(search.getToDate())));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    private static Date nextDay(Date date) {
        return new Date(date.getTime() + 86_400_000L);
    }
}
