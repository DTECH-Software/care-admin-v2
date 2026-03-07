/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 12:02 PM
 * <p>
 */

package com.dtech.admin.repository;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.WebSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebSectionRepository extends JpaRepository<WebSection, Long> , JpaSpecificationExecutor<WebSection> {

    @Query(value = "SELECT new com.dtech.admin.dto.SimpleBaseDTO(s.code,s.description) FROM WebSection s WHERE s.status = :status")
    List<SimpleBaseDTO> findAllBySection(Status status);
}
