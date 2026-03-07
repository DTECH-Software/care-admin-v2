/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 11:42 AM
 * <p>
 */

package com.dtech.admin.mapper.entityToDto;

import com.dtech.admin.dto.response.UsernamePolicyResponseDTO;
import com.dtech.admin.model.WebUsernamePolicy;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

@Log4j2
public class UsernamePolicyMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    public static UsernamePolicyResponseDTO mapUsernamePolicyDetails(WebUsernamePolicy webUsernamePolicy){
        try {
            log.info("mapUserRoleDetails mapper {} ", webUsernamePolicy);
             return modelMapper.map(webUsernamePolicy, UsernamePolicyResponseDTO.class);
        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }
}
