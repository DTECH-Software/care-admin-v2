/**
 * User: Himal_J
 * Date: 4/29/2025
 * Time: 11:20 AM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PageRequestDTO extends ChannelRequestDTO{
    private Long id;
    private String status;
}
