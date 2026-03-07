package com.dtech.admin.validator;

import com.dtech.admin.validator.validators.AgeValidators;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {AgeValidators.class})
public @interface ValidAge {
    String message() default "Age must be below 65.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
