/**
 * User: Himal_J
 * Date: 4/24/2025
 * Time: 11:18 AM
 * <p>
 */

package com.dtech.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponseDTO {
    private String accessToken;
    private String refreshToken;
}
