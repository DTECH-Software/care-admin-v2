UPDATE common_paramter
SET value = 59,
    last_modified_date = CURRENT_TIMESTAMP,
    last_modified_user = 'system'
WHERE code = 'EMPLOYEE_MAX_AGE_FOR_REQUEST_DDF';

