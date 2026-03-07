/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 12:37 PM
 * <p>
 */

package com.dtech.admin.mapper.dtoToEntity;


import com.dtech.admin.dto.request.PasswordPolicyRequestDTO;
import com.dtech.admin.model.WebPasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

@Log4j2
@RequiredArgsConstructor
public class PasswordPolicyMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    public static WebPasswordPolicy mapPasswordPolicy(PasswordPolicyRequestDTO passwordPolicyRequestDTO) {
        try {
            log.info("Password policy mapper start dto to entity");
            WebPasswordPolicy passwordPolicy = modelMapper.map(passwordPolicyRequestDTO, WebPasswordPolicy.class);
            log.info("Success password policy mapper  dto to entity {} ", passwordPolicy);
            return passwordPolicy;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }


}
