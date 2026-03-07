/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 8:39 AM
 * <p>
 */

package com.dtech.admin.validator.validators;

import com.dtech.admin.validator.ValidAge;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;


@Log4j2
public class AgeValidators implements ConstraintValidator<ValidAge, Date> {
    @Override
    public void initialize(ValidAge constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Date dob, ConstraintValidatorContext constraintValidatorContext) {
        log.info("call age validator");

        try {
            if (dob == null) {
                return true;
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy");
            LocalDate givenDate = LocalDate.parse(String.valueOf(dob), formatter);
            LocalDate currentDate = LocalDate.now();
            int age = Period.between(givenDate, currentDate).getYears();
            return age < 65;
        } catch (DateTimeParseException e) {
            log.error(e);
            return false;
        }
    }
}
