/**
 * User: Himal_J
 * Date: 5/5/2025
 * Time: 6:57 AM
 * <p>
 */

package com.dtech.admin.dto.response;

import com.dtech.admin.dto.CommonResponseDTO;
import com.dtech.admin.dto.SimpleBaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CompanyResponseDTO extends CommonResponseDTO {
    private SimpleBaseDTO group;
}
