/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 11:42 AM
 * <p>
 */

package com.dtech.admin.mapper.entityToDto;

import com.dtech.admin.dto.CommonResponseDTO;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.WebPage;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

@Log4j2
public class PageMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    public static CommonResponseDTO mapPageDetails(WebPage webPage){
        try {
            log.info("mapPageDetails mapper {} ", webPage);
            CommonResponseDTO commonResponseDTO = modelMapper.map(webPage, CommonResponseDTO.class);
            commonResponseDTO.setStatusDescription(Status.valueOf(commonResponseDTO.getStatus()).getDescription());
            return commonResponseDTO;
        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }
}
