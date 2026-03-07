/**
 * User: Himal_J
 * Date: 4/30/2025
 * Time: 2:13 PM
 * <p>
 */

package com.dtech.admin.dto.audit;

import lombok.Data;

@Data
public class CompanyAuditDTO {
    public String code;
    public String description;
    public String statusDescription;
    public String group;
    public String groupDescription;

    public String toString() {
        return "Code: " + code +
                ", Description: " + description +
                ", Status: " + statusDescription +
                ", Group: " + groupDescription;
    }
}
