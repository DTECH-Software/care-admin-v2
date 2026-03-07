/**
 * User: Himal_J
 * Date: 5/1/2025
 * Time: 8:19 AM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserRoleRequestDTO extends ChannelRequestDTO {
    private Long id;
    private String code;
    private String description;
    private String status;
}
