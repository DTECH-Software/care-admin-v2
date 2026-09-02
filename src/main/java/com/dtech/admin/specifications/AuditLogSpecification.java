package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.AuditLogSearchDTO;
import com.dtech.admin.model.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class AuditLogSpecification {
    private AuditLogSpecification() { }

    public static Specification<AuditLog> getSpecification(AuditLogSearchDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter == null) return cb.conjunction();
            if (hasText(filter.getSource()) && !"ALL".equalsIgnoreCase(filter.getSource()))
                predicates.add(cb.equal(root.get("source"), filter.getSource().toUpperCase()));
            if (hasText(filter.getModule()))
                predicates.add(cb.equal(cb.lower(root.get("module")), filter.getModule().toLowerCase()));
            if (hasText(filter.getAction()))
                predicates.add(cb.like(cb.lower(root.get("action")), "%" + filter.getAction().toLowerCase() + "%"));
            if (hasText(filter.getResult())) {
                if ("SUCCESS".equalsIgnoreCase(filter.getResult()))
                    predicates.add(cb.or(cb.equal(root.get("result"), "SUCCESS"), cb.isNull(root.get("result"))));
                else
                    predicates.add(cb.equal(root.get("result"), filter.getResult().toUpperCase()));
            }
            if (hasText(filter.getPageCode()))
                predicates.add(cb.equal(root.join("page").get("code"), filter.getPageCode()));
            if (hasText(filter.getTaskCode()))
                predicates.add(cb.equal(root.join("task").get("code"), filter.getTaskCode()));
            if (hasText(filter.getUsername()))
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + filter.getUsername().toLowerCase() + "%"));
            if (hasText(filter.getIpAddress()))
                predicates.add(cb.like(root.get("ipAddress"), "%" + filter.getIpAddress() + "%"));
            if (hasText(filter.getClientAppVersion()))
                predicates.add(cb.equal(root.get("clientAppVersion"), filter.getClientAppVersion()));
            if (hasText(filter.getClientPlatform()))
                predicates.add(cb.equal(root.get("clientPlatform"), filter.getClientPlatform().toUpperCase()));
            if (hasText(filter.getAppUpdateStatus()))
                predicates.add(cb.equal(root.get("appUpdateStatus"), filter.getAppUpdateStatus().toUpperCase()));
            if (filter.getFromDate() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), filter.getFromDate()));
            if (filter.getToDate() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), filter.getToDate()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
