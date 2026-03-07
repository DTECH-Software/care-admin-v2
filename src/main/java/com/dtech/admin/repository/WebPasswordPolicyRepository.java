/**
 * User: Himal_J
 * Date: 4/24/2025
 * Time: 9:29 AM
 * <p>
 */

package com.dtech.admin.repository;

import com.dtech.admin.model.WebPasswordPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebPasswordPolicyRepository extends JpaRepository<WebPasswordPolicy, Long> {
    @Query(value = "SELECT wp FROM WebPasswordPolicy wp")
    Optional<WebPasswordPolicy> findPasswordPolicy();
}
