# 57. Admin Claims Approval View Logic

Status: Current business baseline for BA / QA / client review

## Purpose
Explain what admins see and what validations are repeated before approving claims.

## Business Summary
The admin claim approval view must show claim details, employee context, workflow steps, available limits, and final decision data. Approval actions revalidate important business rules.

## Main Business Rules
- Admin approval view resolves policy period and available limit using the claim employee, claim treatment, claim category, staff category, and effective permanent date.
- Available limits should match claim-service calculation for normal, rejoin, and staff category change scenarios.
- Admin approval revalidates dependent eligibility, claim request period, policy, treatment, and available balance.
- Parent dependent medical claims are blocked for EX-OP1, EX-OP2, MM, and SNR during admin approval.
- L1, L2, and L3 workflow records show status, amount, remark, and approved/rejected date when available.
- Final approval date is taken from the final L2 or L3 decision.
- Rejection date is taken from the final L2 or L3 rejection decision.
- Payment advice status and advice number are displayed where payment advice exists.

## BA Review Points
- Confirm admin validation should block claims submitted before later policy rule changes.
- Confirm which final workflow level should be used when L2 and L3 both exist.
- Confirm approval-view limit display for shared and nested buckets.

## QA Checkpoints
- Compare approval-view limits with claim-service reference-data for the same user.
- Test parent claim restrictions for EX-OP1, EX-OP2, MM, and SNR.
- Test rejected, approved, partial approved, and under-review workflows.

## Client View
- Approvers should see current eligibility and remaining balances before making a final decision.

## Related Topics
- 45. Insurance Claim Approval Workflow
- 48. Approval Amount Validation
- 56. Employee Summary Logic
