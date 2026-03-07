package com.dtech.admin.repository;

import com.dtech.admin.enums.Status;
import com.dtech.admin.model.UserPersonalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPersonalDetailsRepository extends JpaRepository<UserPersonalDetails, Long> , JpaSpecificationExecutor<UserPersonalDetails> {
    boolean existsByEpfNoIgnoreCaseAndUserStatusInAndUserCompanyDetails_companyTypes_code(String epfNo, List<Status> userStatus,String companyCode);
    boolean existsByNicIgnoreCaseAndUserStatusIn(String epfNo, List<Status> userStatus);
    boolean existsByEmailIgnoreCaseAndUserStatusIn(String epfNo, List<Status> userStatus);
}
