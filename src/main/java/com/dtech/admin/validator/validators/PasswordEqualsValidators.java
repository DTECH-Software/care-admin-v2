/**
 * User: Himal_J
 * Date: 2/5/2025
 * Time: 5:01 PM
 * <p>
 */

package com.dtech.admin.validator.validators;

import com.dtech.admin.validator.PasswordEquals;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;

@Log4j2
public class PasswordEqualsValidators implements ConstraintValidator<PasswordEquals, Object> {

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        log.info("Equals validators {}",object);
        Field field1 = null;
        Field field2 = null;
        try {
            log.info("Equals validators get fields {}",object);
            field1 = object.getClass().getDeclaredField("password");
            field2 = object.getClass().getDeclaredField("confirmPassword");
        } catch (NoSuchFieldException e) {
            log.error(e);
            throw new RuntimeException(e);
        }

        field1.setAccessible(true);
        field2.setAccessible(true);

        String fieldValue1 = null;
        String fieldValue2 = null;
        try {
            log.info("Equals validators get fields value {}",object);
            fieldValue1 = (String) field1.get(object);
            fieldValue2 = (String) field2.get(object);
        } catch (IllegalAccessException e) {
            log.error(e);
            throw new RuntimeException(e);
        }

        return fieldValue1 != null && fieldValue1.equals(fieldValue2);
    }

}
