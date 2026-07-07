package com.dtech.admin.dto.response;

import com.dtech.admin.dto.SimpleBaseDTO;
import lombok.Data;

import java.util.Date;

@Data
public class UserCompanyDetailsResponseDTO {
    private SimpleBaseDTO companyTypes;
    private SimpleBaseDTO paymentCompany;
    private SimpleBaseDTO deathPaymentCompany;
    private SimpleBaseDTO staffCategories;
    private SimpleBaseDTO staffTypes;
    private String designation;
    private Date permanentDate;
    private Date previousPermanentDate;
    private Date terminateDate;
    private SimpleBaseDTO insurancePolicy;
    private String facility;
    private String facilityDescription;
    private DocumentDownloadResponseDTO promoDoc;
    private Date transferDate;
    private DocumentDownloadResponseDTO transferDoc;
}
