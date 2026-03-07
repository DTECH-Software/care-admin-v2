/**
 * User: Himal_J
 * Date: 5/2/2025
 * Time: 10:30 AM
 * <p>
 */

package com.dtech.admin.dto.request.validator;

import com.dtech.admin.validator.OnUpdate;
import com.dtech.admin.validator.ValidCommonPolicy;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ValidCommonPolicy(message = "Username policy invalid",groups = {OnUpdate.class})
public class UsernamePolicyRequestValidatorDTO extends CommonPolicyRequestValidatorDTO {

}
