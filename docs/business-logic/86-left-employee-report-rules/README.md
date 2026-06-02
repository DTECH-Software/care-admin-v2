# 86. Left Employee Report Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how the Left Employee Report identifies and filters left employees.

## Business Summary
Left Employee Report is based on employee termination information. It is not based on created date or last modified date.

## Main Business Rules
- A left employee must have a non-null terminate date.
- Default status filter is INACTIVE.
- If status is provided, the provided status list is used.
- Date range filters use user company details terminate date.
- Company, staff category, and facility filters apply from current employee company details.
- Active employees with terminate dates are not included by default unless status filter explicitly allows them.

## BA Review Points
- Confirm whether ACTIVE users with terminate dates should ever appear.
- Confirm whether DELETE status should be excluded permanently.
- Confirm which company details should be used for rejoined employees.

## QA Checkpoints
- Test terminateDateFrom and terminateDateTo.
- Test inactive employee with terminate date.
- Test active employee with terminate date and status filter behavior.
- Test company, staff category, and facility filters.

## Client View
- Left Employee Report should show employees who have officially left according to terminate date and status.

## Related Topics
- 16. Employee Status and Activation/Inactivation
- 17. Employee Company Details Management
