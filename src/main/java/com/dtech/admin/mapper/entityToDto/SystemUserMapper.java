/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 12:37 PM
 * <p>
 */

package com.dtech.admin.mapper.entityToDto;


import com.dtech.admin.dto.SimpleBaseDTO;
import com.dtech.admin.dto.response.DocumentDownloadResponseDTO;
import com.dtech.admin.dto.response.UserCommonResponseDTO;
import com.dtech.admin.dto.response.UserDetailsResponseDTO;
import com.dtech.admin.enums.ApprovalLevel;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.CompanyTypes;
import com.dtech.admin.model.WebUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;

import java.util.Comparator;
import java.util.List;

@Log4j2
@RequiredArgsConstructor
public class SystemUserMapper {
   private final static ModelMapper modelMapper = new ModelMapper();
    public static UserDetailsResponseDTO mapUserDetailsForLogin(WebUser webUser) {
        try {
            log.info("user profile mapper start for login");
//            modelMapper.addMappings(new PropertyMap<WebUser, UserDetailsResponseDTO>() {
//                @Override
//                protected void configure() {
//                    skip(destination.getCompany());
//                    skip(destination.getCompanyGroup());
//                }
//            });

            UserDetailsResponseDTO userDetailsResponseDTO = modelMapper.map(webUser, UserDetailsResponseDTO.class);
            userDetailsResponseDTO.setStatusDescription(Status.valueOf(userDetailsResponseDTO.getStatus()).getDescription());
            applyCompanies(webUser, userDetailsResponseDTO);

            if (webUser.getProfileImg() != null) {
                log.info("user get profile img");
                DocumentDownloadResponseDTO documentDownloadResponseDTO =
                        modelMapper.map(webUser.getProfileImg(), DocumentDownloadResponseDTO.class);
                userDetailsResponseDTO.setProImg(documentDownloadResponseDTO);
                log.info("user get profile img downloaded");
            }

//            if(webUser.getCompanyGroup() != null) {
//                userDetailsResponseDTO.setCompanyGroup(new SimpleBaseDTO(webUser.getCompanyGroup().getCode(),webUser.getCompanyGroup().getDescription()));
//            }else if(webUser.getCompany() != null) {
//                userDetailsResponseDTO.setCompany(new SimpleBaseDTO(webUser.getCompany().getCode(), webUser.getCompany().getDescription()));
//            }

            log.info("Success profile mapper for login {} ", userDetailsResponseDTO);
            return userDetailsResponseDTO;

        } catch (Exception e) {
            log.error("Error mapping user details for login", e);
            throw e;
        }
    }

    public static UserCommonResponseDTO mapUserDetails(WebUser webUser) {
        try {

            log.info("user profile mapper start");
//            modelMapper.addMappings(new PropertyMap<WebUser, UserCommonResponseDTO>() {
//                @Override
//                protected void configure() {
//                    skip(destination.getCompany());
//                    skip(destination.getCompanyGroup());
//                }
//            });

            UserCommonResponseDTO commonResponseDTO = modelMapper.map(webUser, UserCommonResponseDTO.class);
            commonResponseDTO.setStatusDescription(Status.valueOf(commonResponseDTO.getStatus()).getDescription());
            commonResponseDTO.setLoginStatus(Status.valueOf(commonResponseDTO.getLoginStatus()).getDescription());
            commonResponseDTO.setNewUsername(commonResponseDTO.getUsername());
            applyCompanies(webUser, commonResponseDTO);
            if(commonResponseDTO.getApprovalLevel() != null) {
                commonResponseDTO.setIsApprovalLevel(true);
                commonResponseDTO.setApprovalLevelDescription(ApprovalLevel.valueOf(commonResponseDTO.getApprovalLevel()).getDescription());
            }else{
                commonResponseDTO.setIsApprovalLevel(false);
            }

            if(commonResponseDTO.getDeathApprovalLevel() !=null){
                commonResponseDTO.setIsDeathApprovalLevel(true);
                commonResponseDTO.setDeathApprovalLevelDescription(ApprovalLevel.valueOf(commonResponseDTO.getDeathApprovalLevel()).getDescription());
            }else{
                commonResponseDTO.setIsDeathApprovalLevel(false);
            }

//            if(webUser.getCompanyGroup() != null) {
//                commonResponseDTO.setCompanyGroup(new SimpleBaseDTO(webUser.getCompanyGroup().getCode(),webUser.getCompanyGroup().getDescription()));
//            }else if(webUser.getCompany() != null) {
//                commonResponseDTO.setCompany(new SimpleBaseDTO(webUser.getCompany().getCode(), webUser.getCompany().getDescription()));
//            }

            log.info("Success profile mapper {} ", commonResponseDTO);
            return commonResponseDTO;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private static void applyCompanies(WebUser webUser, UserCommonResponseDTO responseDTO) {
        List<SimpleBaseDTO> companies = webUser.getCompanies().stream()
                .sorted(Comparator.comparing(CompanyTypes::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(company -> new SimpleBaseDTO(company.getCode(), company.getDescription()))
                .toList();
        responseDTO.setCompanies(companies);
        responseDTO.setCompany(companies.isEmpty() ? null : companies.get(0));
    }

}
