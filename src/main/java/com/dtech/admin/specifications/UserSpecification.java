/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 10:13 AM
 * <p>
 */

package com.dtech.admin.specifications;

import com.dtech.admin.dto.search.SystemUserSearchDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.model.WebUserRole;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class UserSpecification {
    public static Specification<WebUser> getSpecification(SystemUserSearchDTO filterDto,String username) {
        log.info("User filter: " + filterDto);
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<WebUser, WebUserRole> webUserRoleJoin = root.join("userRole", JoinType.LEFT);

            if (filterDto.getNewUsername() != null && !filterDto.getNewUsername().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), "%" + filterDto.getNewUsername().toLowerCase() + "%"));
            }

            if (filterDto.getNic() != null && !filterDto.getNic().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("nic")), "%" + filterDto.getNic().toLowerCase() + "%"));
            }

            if (filterDto.getEmail() != null && !filterDto.getEmail().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + filterDto.getEmail().toLowerCase() + "%"));
            }

            if (filterDto.getMobile() != null && !filterDto.getMobile().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("mobile")), "%" + filterDto.getMobile().toLowerCase() + "%"));
            }

            if (filterDto.getFirstName() != null && !filterDto.getFirstName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + filterDto.getFirstName().toLowerCase() + "%"));
            }

            if (filterDto.getLastName() != null && !filterDto.getLastName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%" + filterDto.getLastName().toLowerCase() + "%"));
            }

            if (filterDto.getUserRole() != null && !filterDto.getUserRole().isEmpty()) {
                predicates.add(criteriaBuilder.equal(webUserRoleJoin.get("code"), filterDto.getUserRole()));
            }

            if (filterDto.getStatus() != null && !filterDto.getStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), Status.valueOf(filterDto.getStatus())));
            }

            if (filterDto.getLoginStatus() != null && !filterDto.getLoginStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("loginStatus"), Status.valueOf(filterDto.getLoginStatus())));
            }

            predicates.add(root.get("status").in(List.of(Status.ACTIVE, Status.INACTIVE)));
            predicates.add(criteriaBuilder.notEqual(root.get("username"), username));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }

    public static Specification<WebUser> getSpecification(String username) {
        log.info("User filter default :");
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("status").in(List.of(Status.ACTIVE, Status.INACTIVE)));
            predicates.add(criteriaBuilder.notEqual(root.get("username"), username));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }
}
