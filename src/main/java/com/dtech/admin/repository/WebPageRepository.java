package com.dtech.admin.repository;

import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.WebPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebPageRepository extends JpaRepository<WebPage, Long> , JpaSpecificationExecutor<WebPage> {

    Optional<WebPage> findByCode(String code);
    Optional<WebPage> findByCodeAndStatus(String code,Status status);

    @Query(value = "SELECT new com.dtech.admin.dto.SimpleBaseDTO(s.code,s.description) FROM WebPage s WHERE s.status = :status AND s.webSection.code = :webSectionCode ")
    List<SimpleBaseDTO> findAllPagesBySection(Status status,String webSectionCode);
}
