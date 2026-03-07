/**
 * User: Himal_J
 * Date: 5/5/2025
 * Time: 6:38 AM
 * <p>
 */

package com.dtech.admin.dto.search;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CompanySearchDTO extends CommonSearchDTO {
    private String group;
}
