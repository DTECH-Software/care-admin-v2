package com.dtech.admin.repository;

import com.dtech.admin.enums.Status;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.CompanyTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> , JpaSpecificationExecutor<ApplicationUser> {

    Optional<ApplicationUser> findByUserPersonalDetails_EpfNoIgnoreCaseAndUserPersonalDetails_UserCompanyDetails_CompanyTypes_Code(
            String epfNo, String companyCode);

    Optional<ApplicationUser> findByUserPersonalDetails_EpfNoIgnoreCaseAndUserPersonalDetails_UserCompanyDetails_CompanyTypes_CodeAndUserPersonalDetails_UserStatus(
            String epfNo, String companyCode, Status status);

    Optional<ApplicationUser> findTopByUserPersonalDetails_NicIgnoreCaseAndUserPersonalDetails_UserStatusAndIdNotOrderByIdDesc(
            String nic, Status status, Long id);

    Optional<ApplicationUser> findByUserPersonalDetails_EpfNoIgnoreCaseAndUserPersonalDetails_NicIgnoreCaseAndUserPersonalDetails_UserStatus(
            String epfNo, String nic, Status status);
}
