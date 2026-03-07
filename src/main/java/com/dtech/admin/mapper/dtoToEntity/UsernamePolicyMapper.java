/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 12:37 PM
 * <p>
 */

package com.dtech.admin.mapper.dtoToEntity;

import com.dtech.admin.dto.request.UsernamePolicyRequestDTO;
import com.dtech.admin.model.WebUsernamePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

@Log4j2
@RequiredArgsConstructor
public class UsernamePolicyMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    public static WebUsernamePolicy mapUsernamePolicy(UsernamePolicyRequestDTO usernamePolicyRequestDTO) {
        try {
            log.info("Username policy mapper start dto to entity");
            WebUsernamePolicy usernamePolicy = modelMapper.map(usernamePolicyRequestDTO, WebUsernamePolicy.class);
            log.info("Success username policy mapper  dto to entity {} ", usernamePolicy);
            return usernamePolicy;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }


}
