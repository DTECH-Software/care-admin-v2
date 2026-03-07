/**
 * User: Himal_J
 * Date: 3/5/2025
 * Time: 11:23 AM
 * <p>
 */

package com.dtech.admin.validator.validators;

import com.dtech.admin.util.DateTimeUtil;
import com.dtech.admin.validator.ValidPastDays;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

import java.util.Date;
@Log4j2
public class PastDaysValidators implements ConstraintValidator<ValidPastDays, Date> {
    @Override
    public void initialize(ValidPastDays constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Date date, ConstraintValidatorContext constraintValidatorContext) {
        try {
            log.info("Check past days {} ", date);

            return date != null && date.before(DateTimeUtil.getCurrentDateTime());

        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
