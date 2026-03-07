/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 2:48 PM
 * <p>
 */

package com.dtech.admin.dto.request;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ChannelRequestDTO {
    private String ip;
    private String message;
    private String username;
    private String userAgent;
}
