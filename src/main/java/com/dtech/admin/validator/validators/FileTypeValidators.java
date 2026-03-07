package com.dtech.admin.validator.validators;


import com.dtech.admin.validator.ValidFileType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FileTypeValidators implements ConstraintValidator<ValidFileType, String> {

    private static final String[] ALLOWED_CONTENT_TYPES = {
        "image/png",
        "image/jpeg",
        "image/jpg",
        "application/pdf"
    };

    @Override
    public void initialize(ValidFileType constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String fileType, ConstraintValidatorContext constraintValidatorContext) {
       log.info("validating file");
       try {
           if(fileType == null) {
               return false;
           }
           for (String allowedType : ALLOWED_CONTENT_TYPES) {
               if (fileType.equalsIgnoreCase(allowedType)) {
                   return true;
               }
           }

           return false;

       }catch (Exception e) {
           log.error(e);
           throw e;
       }
    }
}