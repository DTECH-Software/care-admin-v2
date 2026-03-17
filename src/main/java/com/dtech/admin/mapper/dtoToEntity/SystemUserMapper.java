/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 12:37 PM
 * <p>
 */

package com.dtech.admin.mapper.dtoToEntity;


import com.dtech.admin.dto.request.SystemUserRequestDTO;
import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

@Log4j2
@RequiredArgsConstructor
public class SystemUserMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    static {
        modelMapper.typeMap(SystemUserRequestDTO.class, WebUser.class)
                .addMappings(mp -> mp.skip(WebUser::setCompanies));
    }

    public static WebUser mapUserForAdd(SystemUserRequestDTO systemUserRequestDTO) {
        try {
            log.info("user profile mapper start dto to entity for login");
            WebUser webUser = modelMapper.map(systemUserRequestDTO, WebUser.class);
            webUser.setUsername(systemUserRequestDTO.getNewUsername());
            webUser.setLoginStatus(Status.ACTIVE);
            webUser.setStatus(Status.INACTIVE);
            webUser.setReset(true);
            webUser.setPasswordExpiredDate(DateTimeUtil.get1HourFutureDate());
            webUser.setAttemptCount(0);
            webUser.setExpectingFirstTimeLogging(true);
            if(systemUserRequestDTO.getIsApprovalLevel()){
                webUser.setApprovalLevel(ApprovalLevel.valueOf(systemUserRequestDTO.getApprovalLevel()));
            }

            if(systemUserRequestDTO.getIsDeathApprovalLevel()){
                webUser.setApprovalLevel(ApprovalLevel.valueOf(systemUserRequestDTO.getDeathApprovalLevel()));
            }

            log.info("Success profile mapper  dto to entity for login {} ", webUser);
            return webUser;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    public static void mapUser(SystemUserRequestDTO systemUserRequestDTO, WebUser webUser) {
        try {
            log.info("user profile mapper start dto to entity");
             modelMapper.map(systemUserRequestDTO,webUser);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }


}
