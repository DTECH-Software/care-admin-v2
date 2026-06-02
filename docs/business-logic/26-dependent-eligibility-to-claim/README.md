# 26. Dependent Eligibility to Claim

Status: Current business baseline for BA / QA / client review

## Purpose
Explain when a dependent can be selected for a medical or death claim.

## Business Summary
Dependent claim eligibility is stricter than dependent registration. A dependent can be registered but still not be eligible for a medical claim because of approval status, facility, previous death claim, age, treatment, staff category, or relation.

## Medical Claim Rules In Care App
- The claim must belong to an active employee with an assigned active insurance policy.
- If the claim is for a dependent, the dependent id must belong to the same employee.
- The dependent must have status `APPROVED`.
- The dependent eligible facility must be `INSURANCE` or `BOTH`.
- If an `APPROVED` death claim already exists for the same employee and dependent, the medical claim is blocked.
- Dependent `CHILDREN` medical claims are blocked when child age is greater than 25.
- Normal Staff (`NS`) unmarried parent medical claims are blocked when parent age is greater than 65.
- Parent age exactly 65 is currently allowed because the implementation checks `age > 65`.
- Parent medical claims are fully blocked for staff categories `EX-OP1`, `EX-OP2`, `MM`, and `SNR`.
- Dependent CRIC restriction exists for staff codes `MM`, `EX-01`, and `EX-02` in the current claim-service code.
- If the employee is unmarried and staff category is not `NS`, dependent medical claims are blocked.

## Admin Approval Revalidation Rules
- Care Admin revalidates dependent claim eligibility during claim approval.
- Temporary users and employees with facility `DEATH` are blocked from medical claim approval.
- A missing or inactive insurance policy blocks approval.
- Dependent CRIC claims are blocked during admin approval.
- The dependent must still be `APPROVED`, assigned to the same employee, and facility-eligible for `INSURANCE` or `BOTH`.
- Parent medical claims are blocked for `EX-OP1`, `EX-OP2`, `MM`, and `SNR`.
- If an approved death claim exists for the dependent, the medical claim cannot be approved.
- Current Care Admin approval revalidates the restricted-staff parent rule and death-claim rule. The Normal Staff parent age and child age checks are enforced in Care App claim creation and facility recheck.

## Parent Claim Rules
- Direct parent claims normally map to `FATHER` or `MOTHER` relations with dependent category `PARENTS`.
- Normal Staff (`NS`) unmarried employees can use parents for medical claims only while parent age is 65 or below.
- When parent age becomes greater than 65, Care App scheduled facility recheck moves that dependent to `DEATH`, which prevents future medical claim selection.
- `EX-OP1`, `EX-OP2`, `MM`, and `SNR` cannot create or approve parent medical claims regardless of parent age.
- In-law relations are not the same as direct parent dependent category in the current claim rule; they should be treated separately unless BA confirms they must follow the same parent restriction.

## Child Claim Rules
- Child medical eligibility is based on dependent category `CHILDREN`.
- Child age is calculated from dependent date of birth.
- Child medical claim is blocked when age is greater than 25.
- Child age exactly 25 is currently allowed because the implementation checks `age > 25`.
- Scheduled facility recheck moves child relation `CHILD` to `DEATH` facility when age is greater than 25.

## Death Claim Dependency Rules
- Death claims use dependent eligibility separately from medical claim eligibility.
- A dependent death claim requires an approved dependent with eligible facility `DEATH` or `BOTH`.
- Once a dependent death claim is finally approved, the dependent `liveStatus` is set to false.
- After an approved dependent death claim exists, medical claim usage for that dependent is blocked.

## BA Review Points
- Confirm parent claim restriction for EX-OP1, EX-OP2, MM, and SNR.
- Confirm whether Normal Staff parent age should block above 65 only, or 65 and above.
- Confirm whether in-law relations should follow parent rules or spouse-family rules.
- Confirm death claim live-status impact before medical claim selection.

## QA Checkpoints
- Submit parent claim for EX-OP1, EX-OP2, MM, and SNR and verify rejection.
- Submit Normal Staff unmarried parent claim with age 65 and verify current behavior allows it.
- Submit Normal Staff unmarried parent claim with age 66 and verify rejection.
- Verify admin approval blocks an invalid parent dependent claim.
- Verify child age above 25 is rejected.

## Client View
- Staff categories with no parent medical cover should not be able to claim for parents.

## Related Topics
- 24. Dependent Eligibility Rules
- 45. Insurance Claim Approval Workflow
- 46. Death Claim Approval Workflow
