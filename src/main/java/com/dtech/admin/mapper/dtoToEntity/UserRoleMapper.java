/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 12:37 PM
 * <p>
 */

package com.dtech.admin.mapper.dtoToEntity;


import com.dtech.admin.dto.request.UserRoleRequestDTO;

import com.dtech.admin.model.WebUserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

@Log4j2
@RequiredArgsConstructor
public class UserRoleMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    public static WebUserRole mapUserRole(UserRoleRequestDTO userRoleRequestDTO) {
        try {
            log.info("user role mapper start dto to entity");
            WebUserRole webUserRole = modelMapper.map(userRoleRequestDTO, WebUserRole.class);
            log.info("Success user role mapper  dto to entity {} ", webUserRole);
            return webUserRole;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }


}
