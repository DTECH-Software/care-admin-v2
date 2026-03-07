/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 12:45 PM
 * <p>
 */

package com.dtech.admin.validator.validators;


import com.dtech.admin.validator.ValidEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EnumValidators implements ConstraintValidator<ValidEnum, String> {

    private Enum<?>[] enums;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
      this.enums = constraintAnnotation.enumClass().getEnumConstants();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        log.info("EnumValidators {}",value);

        if(value == null){
            log.info("EnumValidators is invalid null value {}",value);
            return true;
        }

        for (Enum<?> enumValue : enums) {
            if(enumValue.name().equals(value)){
                log.info("EnumValidators is valid {}",true);
                return true;
            }
        }
        log.info("EnumValidators is invalid {}",value);
        return false;
    }
}
