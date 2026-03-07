/**
 * User: Himal_J
 * Date: 3/5/2025
 * Time: 11:21 AM
 * <p>
 */

package com.dtech.admin.validator;

import com.dtech.admin.validator.validators.PastDaysValidators;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PastDaysValidators.class)
@Target({ ElementType.TYPE ,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPastDays {
    String message() default "Date must be in the past";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
