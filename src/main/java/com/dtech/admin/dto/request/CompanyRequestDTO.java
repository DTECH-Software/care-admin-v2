/**
 * User: Himal_J
 * Date: 5/5/2025
 * Time: 6:34 AM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CompanyRequestDTO extends ChannelRequestDTO{
    private Long id;
    private String code;
    private String description;
    private String status;
    private String group;

}
