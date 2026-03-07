package com.dtech.admin.validator.validators;

import com.dtech.admin.dto.request.validator.CommonPolicyRequestValidatorDTO;
import com.dtech.admin.validator.ValidCommonPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CommonPolicyValidator implements ConstraintValidator<ValidCommonPolicy, CommonPolicyRequestValidatorDTO> {

    @Override
    public boolean isValid(CommonPolicyRequestValidatorDTO value, ConstraintValidatorContext context) {

        log.info("CommonPolicyValidator called {}", value);

        if (value == null) return true;

        context.disableDefaultConstraintViolation();
        boolean valid = true;

        int max = value.getMaxLength();
        int min = value.getMinLength();

        int upper = value.getMinUpperCase();
        int lower = value.getMinLowerCase();
        int numbers = value.getMinNumbers();
        int special = value.getMinSpecialCharacters();

        int sum = upper + lower + numbers + special;

        if (upper > max) {
            log.info("CommonPolicyValidator upper > max {} {} ", upper, max);
            context.buildConstraintViolationWithTemplate("Min upper case must be <= max length")
                .addPropertyNode(String.valueOf(value.getMinUpperCase())).addConstraintViolation();
            valid = false;
        }

        if (lower > max) {
            log.info("CommonPolicyValidator lower > max {} {} ", lower, max);
            context.buildConstraintViolationWithTemplate("Min lower case must be <= max length")
                .addPropertyNode(String.valueOf(value.getMinLowerCase())).addConstraintViolation();
            valid = false;
        }

        if (numbers > max) {
            log.info("CommonPolicyValidator numbers > max {} {} ", numbers, max);
            context.buildConstraintViolationWithTemplate("Min numbers must be <= max length")
                .addPropertyNode(String.valueOf(value.getMinNumbers())).addConstraintViolation();
            valid = false;
        }

        if (special > max) {
            log.info("CommonPolicyValidator special > max {} {} ", special, max);
            context.buildConstraintViolationWithTemplate("Min special characters must be <= max length")
                .addPropertyNode(String.valueOf(value.getMinSpecialCharacters())).addConstraintViolation();
            valid = false;
        }

        if (sum < min) {
            log.info("CommonPolicyValidator sum < minLen {} {} ", sum, min);
            context.buildConstraintViolationWithTemplate("Sum of min character requirements must be >= minLength")
                .addPropertyNode(String.valueOf(value.getMinLength())).addConstraintViolation();
            valid = false;
        }

        if (min > max) {
            log.info("CommonPolicyValidator minLen > max {} {} ", min, max);
            context.buildConstraintViolationWithTemplate("Min length must be <= max length")
                    .addPropertyNode(String.valueOf(value.getMinLength())).addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
