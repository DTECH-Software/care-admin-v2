package com.dtech.admin.validator.validators;

import com.dtech.admin.validator.Conditional;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Arrays;

import static org.springframework.util.ObjectUtils.isEmpty;

@Log4j2
public class ConditionalValidators implements ConstraintValidator<Conditional, Object> {

    private String selected;
    private String[] required;
    private String message;
    private String[] values;

    @Override
    public void initialize(Conditional requiredIfChecked) {
        selected = requiredIfChecked.selected();
        required = requiredIfChecked.required();
        message = requiredIfChecked.message();
        values = requiredIfChecked.values();
    }

    @Override
    public boolean isValid(Object objectToValidate, ConstraintValidatorContext context) {
        log.debug("Checking if condition is valid");
        Boolean valid = true;
        BeanWrapper beanWrapper = new BeanWrapperImpl(objectToValidate);
        Object actualValue = beanWrapper.getPropertyValue(selected);
        if (Arrays.asList(values).contains(actualValue)) {
            log.debug("Condition is valid");
            for (String propName : required) {
                beanWrapper = new BeanWrapperImpl(objectToValidate);
                Object requiredValue = beanWrapper.getPropertyValue(propName);
                valid = requiredValue != null && !isEmpty(requiredValue);
                if (!valid) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(message).addPropertyNode(propName).addConstraintViolation();
                }
            }
        }
        return valid;
    }
}