# 56. Employee Summary Logic

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how employee summary balances and claim lists are calculated for admins.

## Business Summary
Employee summary shows claims and balances for an employee, including carry-forward effects from rejoin and staff category change scenarios.

## Main Business Rules
- Summary finds the active employee record and ignores deleted duplicate identities where possible.
- Balances are calculated from current policy period, staff category, treatment, treatment category, and approved utilization.
- Rejoin and promotion carry-forward logic can include approved usage from a previous staff category or previous inactive profile where business rules require it.
- For shared treatment buckets, usage can reduce the treatment-level available amount across categories.
- Under-review claims show submitted amount as the actual submitted value.
- Under-review claims show approved value as a dash because final approval has not happened.
- Rejected claims show approved value as 0.00 when no approved amount exists.
- Final remarks should show L2 or L3 decision remarks where available.

## BA Review Points
- Confirm which previous employee profiles should be carried forward during rejoin.
- Confirm promotion carry-forward for EX-OP1 to MM, MM to SNR, and other category changes.
- Confirm under-review approved value display as dash and rejected approved value as 0.00.

## QA Checkpoints
- Test active employee with deleted duplicate NIC/EPF records.
- Test Normal Staff shared buckets and executive category-specific buckets.
- Test promotion and rejoin balances against claim-service reference-data balances.
- Verify under-review and rejected display values.

## Client View
- Admin summary should show the same remaining balance logic as the employee-facing claim request screens.

## Related Topics
- 35. Available Balance Calculation
- 36. Staff Category Change Carry Forward Logic
- 57. Admin Claims Approval View Logic
