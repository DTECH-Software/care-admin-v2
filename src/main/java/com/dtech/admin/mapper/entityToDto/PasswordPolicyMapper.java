/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 11:42 AM
 * <p>
 */

package com.dtech.admin.mapper.entityToDto;

import com.dtech.admin.dto.response.PasswordPolicyResponseDTO;
import com.dtech.admin.model.WebPasswordPolicy;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

@Log4j2
public class PasswordPolicyMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    public static PasswordPolicyResponseDTO mapPasswordPolicyDetails(WebPasswordPolicy webPasswordPolicy){
        try {
            log.info("mapUserRoleDetails mapper {} ", webPasswordPolicy);
             return modelMapper.map(webPasswordPolicy, PasswordPolicyResponseDTO.class);
        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }
}
