package com.dtech.admin.repository;

import com.dtech.admin.model.UserCompanyDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCompanyDetailsRepository extends JpaRepository<UserCompanyDetails, Long> {
}
