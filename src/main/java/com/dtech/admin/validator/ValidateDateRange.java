package com.dtech.admin.validator;

import com.dtech.admin.validator.validators.DateRangeValidators;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = DateRangeValidators.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateDateRange {
    String message() default "From date must be before To date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String selected() default "";
    String[] values() default {};
}
