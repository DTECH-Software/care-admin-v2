/**
 * User: Himal_J
 * Date: 3/5/2025
 * Time: 10:59 AM
 * <p>
 */

package com.dtech.admin.validator.validators;

import com.dtech.admin.validator.ValidateDateRange;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.util.Date;

@Log4j2
public class DateRangeValidators implements ConstraintValidator<ValidateDateRange, Object> {

    @Override
    public void initialize(ValidateDateRange constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        try {
            log.info("Date range validation started for object: {}", object);

            Field treatmentCategoryField = getField(object, "treatment");

            if (treatmentCategoryField == null) {
                log.error("treatmentCategory field not found in object: {}", object);
                return false;
            }

            treatmentCategoryField.setAccessible(true);
            String treatmentCategory = (String) treatmentCategoryField.get(object);

            log.info("treatmentCategory value: {}", treatmentCategory);

            if ("OUTDOOR".equals(treatmentCategory)) {
                log.info("Skipping date range validation as treatmentCategory is not 'OTHER'");
                return true; // Valid if treatmentCategory is not "OTHER"
            }

            Field fromDateField = getField(object, "fromDate");
            Field toDateField = getField(object, "toDate");

            if (fromDateField == null || toDateField == null) {
                log.error("Required fields (fromDate or toDate) not found in object: {}", object);
                return false;
            }

            fromDateField.setAccessible(true);
            toDateField.setAccessible(true);

            Date fromDate = (Date) fromDateField.get(object);
            Date toDate = (Date) toDateField.get(object);

            log.info("Comparing fromDate: {} and toDate: {}", fromDate, toDate);
            if (fromDate == null || toDate == null) {
                log.error("fromDate or toDate is null for object: {}", object);
                return false;
            }

            return !fromDate.after(toDate);

        } catch (Exception e) {
            log.error("Error during date range validation for object: {}", object, e);
            return false;
        }
    }

    private Field getField(Object object, String fieldName) {
        try {
            return object.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            log.error("Field {} not found in class {}", fieldName, object.getClass().getName(), e);
            return null;
        }
    }
}

