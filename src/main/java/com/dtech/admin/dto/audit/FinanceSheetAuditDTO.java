package com.dtech.admin.dto.audit;

import lombok.Data;

import java.util.Date;

@Data
public class FinanceSheetAuditDTO {
    private String company;
    private String companyDescription;
    private String month;
    private String monthDescription;
    private String year;
    private String yearDescription;
    private String status;
    private String statusDescription;
    private String rejectReason;
    private Date authDateTime;
    private String authUser;
 //   private List<SheetDetailsAuditDTO> sheetDetails;

    public String toString() {
        return "Company: " + companyDescription +
                ", Month: " + monthDescription +
                ", Status: " + statusDescription +
                ", Reject reason: " + rejectReason +
                ", Auth date: " + authDateTime +
                ", Auth user: " + authUser +
                ", Year: " + yearDescription ;
              //  ", Sheets : " + sheetDetails ;
    }

}
