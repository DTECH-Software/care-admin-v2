# 87. Employee List Report Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how Employee List Report filters employee records.

## Business Summary
Employee List Report is based on current employee master data and current company details. Its date range is based on permanent date.

## Main Business Rules
- Date range filters use user company details permanent date.
- permanentDateFrom filters permanent date greater than or equal to the start of the day.
- permanentDateTo filters permanent date less than or equal to the end of the day.
- The report does not use employee created date, last modified date, previous permanent date, or terminate date for this date range.
- Default status filter includes ACTIVE and INACTIVE employees.
- Company, staff category, and facility filters apply to current company details.

## BA Review Points
- Confirm whether previous permanent date should ever be available as a separate filter.
- Confirm whether deleted employees should be excluded from default output.
- Confirm whether temporary employees should appear based on staff type or facility.

## QA Checkpoints
- Test permanentDateFrom and permanentDateTo boundaries.
- Test ACTIVE and INACTIVE default behavior.
- Test company, staff category, and facility filters.
- Verify created date changes do not affect date range results.

## Client View
- Employee List Report date range means permanent date range.

## Related Topics
- 14. Employee Creation
- 19. Employee Permanent Date Handling
- 20. Previous Permanent Date Handling
