/**
 * User: Himal_J
 * Date: 2/10/2025
 * Time: 4:32 PM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;


@Data
public class MessageRequestDTO {
    private String mobileNo;
    private String type;
    private String value;
}
