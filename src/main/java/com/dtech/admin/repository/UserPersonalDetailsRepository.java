package com.dtech.admin.repository;

import com.dtech.admin.enums.Status;
import com.dtech.admin.model.UserPersonalDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPersonalDetailsRepository extends JpaRepository<UserPersonalDetails, Long> , JpaSpecificationExecutor<UserPersonalDetails> {
    boolean existsByEpfNoIgnoreCaseAndUserStatusInAndUserCompanyDetails_companyTypes_code(String epfNo, List<Status> userStatus,String companyCode);
    boolean existsByNicIgnoreCaseAndUserStatusIn(String epfNo, List<Status> userStatus);
    boolean existsByEmailIgnoreCaseAndUserStatusIn(String epfNo, List<Status> userStatus);
    boolean existsByMobileNoIgnoreCaseAndUserStatusIn(String mobileNo, List<Status> userStatus);
    boolean existsByMobileNoIgnoreCaseAndUserStatusInAndIdNot(String mobileNo, List<Status> userStatus, Long id);
    boolean existsByMobileNo(String mobileNo);
    @Query("select u.mobileNo from UserPersonalDetails u where u.mobileNo like concat(:prefix, '%') order by u.mobileNo desc")
    List<String> findLatestMobileNoByPrefix(@Param("prefix") String prefix, Pageable pageable);
    List<UserPersonalDetails> findAllByNicIgnoreCaseAndUserStatusAndIdNotOrderByIdDesc(String nic, Status userStatus, Long id);
    List<UserPersonalDetails> findAllByNicIgnoreCaseAndUserStatusOrderByIdDesc(String nic, Status userStatus);
}
