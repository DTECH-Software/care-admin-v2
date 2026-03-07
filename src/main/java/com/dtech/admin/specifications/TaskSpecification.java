/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 10:13 AM
 * <p>
 */

package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.CommonSearchDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.WebTask;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class TaskSpecification {
    public static Specification<WebTask> getSpecification(CommonSearchDTO filterDto) {
        log.info("Task filter: " + filterDto);
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filterDto.getCode() != null && !filterDto.getCode().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), "%" + filterDto.getCode().toLowerCase() + "%"));
            }

            if (filterDto.getDescription() != null && !filterDto.getDescription().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + filterDto.getDescription().toLowerCase() + "%"));
            }

            if (filterDto.getStatus() != null &&  !filterDto.getStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), Status.valueOf(filterDto.getStatus())));
            }

            predicates.add(root.get("status").in(List.of(Status.ACTIVE, Status.INACTIVE)));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }

    public static Specification<WebTask> getSpecification() {
        log.info("Task filter default :");
        return (root, query,criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("status").in(List.of(Status.ACTIVE, Status.INACTIVE)));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }
}
