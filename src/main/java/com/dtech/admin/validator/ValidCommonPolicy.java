package com.dtech.admin.validator;

import com.dtech.admin.validator.validators.CommonPolicyValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CommonPolicyValidator.class)
@Documented
public @interface ValidCommonPolicy {
    String message() default "Policy values are not valid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}