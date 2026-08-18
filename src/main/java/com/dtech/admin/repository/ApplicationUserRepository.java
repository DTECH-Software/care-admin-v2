package com.dtech.admin.repository;

import com.dtech.admin.enums.Status;
import com.dtech.admin.enums.Gender;
import com.dtech.admin.model.ApplicationUser;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.UserPersonalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> , JpaSpecificationExecutor<ApplicationUser> {

    long countByUserPersonalDetails_Gender(Gender gender);
    long countByUserPersonalDetails_UserCompanyDetails_CompanyTypes_Code(String companyCode);
    long countByUserPersonalDetails_UserCompanyDetails_StaffCategories_Code(String staffCategoryCode);

    Optional<ApplicationUser> findByUserPersonalDetails_EpfNoIgnoreCaseAndUserPersonalDetails_UserCompanyDetails_CompanyTypes_Code(
            String epfNo, String companyCode);

    List<ApplicationUser> findAllByUserPersonalDetails_EpfNoIgnoreCaseAndUserPersonalDetails_UserCompanyDetails_CompanyTypes_Code(
            String epfNo, String companyCode);

    List<ApplicationUser> findAllByUserPersonalDetails_EpfNoIgnoreCaseAndUserPersonalDetails_UserCompanyDetails_CompanyTypes_CodeAndUserPersonalDetails_UserStatusNot(
            String epfNo, String companyCode, Status status);

    Optional<ApplicationUser> findByUserPersonalDetails_EpfNoIgnoreCaseAndUserPersonalDetails_UserCompanyDetails_CompanyTypes_CodeAndUserPersonalDetails_UserStatus(
            String epfNo, String companyCode, Status status);

    Optional<ApplicationUser> findTopByUserPersonalDetails_NicIgnoreCaseAndUserPersonalDetails_UserStatusAndIdNotOrderByIdDesc(
            String nic, Status status, Long id);

    List<ApplicationUser> findAllByUserPersonalDetails_NicIgnoreCaseAndUserPersonalDetails_UserStatusNotAndIdNotOrderByIdDesc(
            String nic, Status status, Long id);

    Optional<ApplicationUser> findByUserPersonalDetails_EpfNoIgnoreCaseAndUserPersonalDetails_NicIgnoreCaseAndUserPersonalDetails_UserStatus(
            String epfNo, String nic, Status status);

    Optional<ApplicationUser> findByUserPersonalDetails(UserPersonalDetails userPersonalDetails);
    boolean existsByPrimaryEmailIgnoreCaseAndUserPersonalDetails_IdNot(String primaryEmail, Long userPersonalDetailsId);
    boolean existsByPrimaryMobile(String primaryMobile);
    @Query("select u.primaryMobile from ApplicationUser u where u.primaryMobile like concat(:prefix, '%') order by u.primaryMobile desc")
    List<String> findLatestPrimaryMobileByPrefix(@Param("prefix") String prefix, Pageable pageable);
}
