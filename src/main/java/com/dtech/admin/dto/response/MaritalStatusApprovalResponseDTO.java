package com.dtech.admin.dto.response;
import com.dtech.admin.model.Document;
import lombok.Data;
import java.util.List;

@Data
public class MaritalStatusApprovalResponseDTO {
    private Long id;
    private String status;
    private String statusDescription;
    private String maritalStatus;
    private String maritalStatusDescription;
    private List<DocumentDownloadResponseDTO> documents;
    private String  employeeName;
    private String nic;
    private String epfNo;
    private String company;
    private String staffCategory;
}
