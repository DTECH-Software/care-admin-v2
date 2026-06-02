# 24. Dependent Eligibility Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain which dependent records can be created, approved, and used in the system.

## Business Summary
Dependent eligibility is controlled by employee marital status, employee gender, dependent relation category, dependent category, duplicate relation rules, supporting documents, approval status, live status, and eligible facility. Care App creates the dependent request; Care Admin approves or rejects it and revalidates the same core data before approval.

## Creation Rules From Care App
- Only an active application user can add dependents.
- New dependents are saved for approval and are not claimable until status becomes `APPROVED`.
- Unmarried employees cannot add `WIFE`, `HUSBAND`, `FATHER_IN_LAW`, `MOTHER_IN_LAW`, or `CHILD`.
- A male employee cannot add relation `HUSBAND`.
- A female employee cannot add relation `WIFE`.
- Relations `HUSBAND`, `FATHER`, `BROTHER`, and `FATHER_IN_LAW` cannot be submitted with dependent gender `FEMALE`.
- Relations `WIFE`, `MOTHER`, `SISTER`, and `MOTHER_IN_LAW` cannot be submitted with dependent gender `MALE`.

## Duplicate Rules
- `MOTHER` and `FATHER` are blocked when the same employee already has an `APPROVED` or `UNDER_REVIEW` dependent for that relation in Care App.
- During Care Admin approval, the duplicate parent check blocks against already `APPROVED` parent dependents.
- `WIFE`, `HUSBAND`, `FATHER_IN_LAW`, and `MOTHER_IN_LAW` are checked by married-round id.
- Care App blocks spouse/in-law duplicates when an `APPROVED` or `UNDER_REVIEW` record exists for the same married round.
- Care Admin approval blocks spouse/in-law duplicates when an `APPROVED` record exists for the same married round.

## Document Rules
- `WIFE` and `HUSBAND` require exactly 2 documents: `BIRTH` and `MARRIED`.
- `MOTHER`, `FATHER`, `CHILD`, `BROTHER`, and `SISTER` require exactly 1 document: `BIRTH`.
- Missing birth certificate, missing marriage certificate, or incorrect document count blocks the request/approval.

## Eligible Facility Rules
- When the employee is married, `SPOUSE` and `CHILDREN` dependents are saved with eligible facility `BOTH`.
- When the employee is unmarried, direct `FATHER` and `MOTHER` relations are saved with eligible facility `BOTH`.
- Other dependent relations are saved with eligible facility `DEATH`.
- Medical dependent claims require eligible facility `INSURANCE` or `BOTH`.
- Death dependent claims require eligible facility `DEATH` or `BOTH`.

## Live Status Rules
- New dependents are saved with `liveStatus = true`.
- When a dependent death claim becomes finally `APPROVED`, Care Admin sets the dependent `liveStatus` to `false`.
- Rejected or under-review death claims do not change dependent live status.

## Scheduled Facility Recheck In Care App
- Care App periodically rechecks approved, under-review, and rejected dependents.
- Child relation `CHILD` is moved to `DEATH` facility when age is greater than 25.
- Normal Staff (`NS`) unmarried parent dependents are moved to `DEATH` facility when parent age is greater than 65.
- Non-`NS` dependents are moved to `DEATH` facility when dependent age is greater than 70.
- The current age checks are `age > limit`; age exactly equal to the limit is not blocked by this scheduled rule.

## BA Review Points
- Confirm allowed relation categories for unmarried and married employees.
- Confirm duplicate checks for parents, spouse, in-laws, and children.
- Confirm document requirements for each relation category.
- Confirm when dependent live status should become inactive.

## QA Checkpoints
- Test unmarried employee spouse, in-law, and child restrictions.
- Test duplicate father/mother and duplicate spouse by married round.
- Test missing document scenarios.
- Test approved dependent death claim impact on live status.

## Client View
- Dependents should only become usable after correct details, documents, and approval are completed.

## Related Topics
- 21. Dependent Creation
- 23. Dependent Approval/Rejection
- 26. Dependent Eligibility to Claim
