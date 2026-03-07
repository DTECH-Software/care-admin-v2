package com.dtech.admin.validator;

import com.dtech.admin.validator.validators.FileTypeValidators;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = FileTypeValidators.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFileType {
    String message() default "Invalid file type. Only PNG, JPEG, JPG, and PDF are allowed.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}