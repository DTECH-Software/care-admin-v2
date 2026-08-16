package com.dtech.admin.specifications;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class CompanyScopeSpecification {
    private CompanyScopeSpecification() { }

    public static <T> Specification<T> companyCodeIn(Collection<String> companyCodes, String... attributePath) {
        return (root, query, cb) -> {
            if (companyCodes == null || companyCodes.isEmpty()) return cb.disjunction();
            Path<?> companyCode = root;
            for (String attribute : attributePath) companyCode = companyCode.get(attribute);
            return cb.upper(companyCode.as(String.class)).in(
                    companyCodes.stream().map(String::trim).map(String::toUpperCase).toList());
        };
    }
}
