/**
 * User: Himal_J
 * Date: 4/30/2025
 * Time: 10:53 AM
 * <p>
 */

package com.dtech.admin.repository;

import com.dtech.admin.model.WebUsernamePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebUsernamePolicyRepository extends JpaRepository<WebUsernamePolicy, Long> {

    @Query(value = "SELECT app FROM WebUsernamePolicy app")
    Optional<WebUsernamePolicy> findUsernamePolicy();
}
