/**
 * User: Himal_J
 * Date: 5/7/2025
 * Time: 8:07 AM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class ExcelUploadRequestDTO extends ChannelRequestDTO{
    private String company;
    private String workBookType;
    private String year;
    private String month;
}
