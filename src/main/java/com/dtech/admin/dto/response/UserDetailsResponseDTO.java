/**
 * User: Himal_J
 * Date: 4/25/2025
 * Time: 9:11 AM
 * <p>
 */

package com.dtech.admin.dto.response;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserDetailsResponseDTO extends UserCommonResponseDTO{
    private boolean isReset;
    private Date lastLoggedDate;
    private boolean expectingFirstTimeLogging;
    private Date passwordExpiredDate;
    private DocumentDownloadResponseDTO proImg;

}
